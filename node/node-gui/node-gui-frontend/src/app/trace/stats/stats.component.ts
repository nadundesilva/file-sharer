import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TableDataSource, TraceableNode} from '../../commons';
import {Subscription} from 'rxjs/Subscription';
import {Observable} from 'rxjs/Observable';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';

class SerMessage {
  query: string;
  startTimeStamp: number;
  firstHitTimeStamp: number;
  messagesCount: number;
  hopCounts: number[];
}

class SerSuperPeerMessage {
  startTimeStamp: number;
  firstHitTimeStamp: number;
  messagesCount: number;
  hopCounts: number[];
}

class History {
  startUpTimeStamp: number;
  nodeSerMessages: Map<TraceableNode, Map<number, SerMessage>>;
  nodeSerSuperPeerMessages: Map<TraceableNode, Map<number, SerSuperPeerMessage>>;
  bootstrappingMessageCount: number;
  maintenanceMessageCount: number;
}

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html'
})
export class StatsComponent implements OnInit, OnDestroy {
  historyFetchSubscription: Subscription;
  timeStampUpdateSubscription: Subscription;

  formattedElapsedTime: string;
  elapsedTime: number;

  startUpTimeStamp: number;
  serMessages: [TraceableNode, number, SerMessage][];
  serSuperPeerMessages: [TraceableNode, number, SerSuperPeerMessage][];
  bootstrappingMessageCount: number;
  maintenanceMessageCount: number;

  serDisplayedColumns = [
    'node', 'sequence-number', 'query', 'messages-count', 'min-hop-count', 'max-hop-count', 'average-hop-count',
    'standard-deviation'
  ];
  serSuperPeerDisplayedColumns = [
    'node', 'sequence-number', 'messages-count', 'min-hop-count', 'max-hop-count', 'average-hop-count',
    'standard-deviation'
  ];

  serMessagesDataSource: TableDataSource<[number, SerMessage]>;
  serSuperPeerMessagesDataSource: TableDataSource<[number, SerSuperPeerMessage]>;

  generalStatisticsDataSource: TableDataSource<[string, string]>;
  serMessageStatisticsDataSource: TableDataSource<[string, string]>;
  serSuperPeerMessageStatisticsDataSource: TableDataSource<[string, string]>;

  @Input()
  nodes: TraceableNode[];

  constructor(private http: HttpClient, private router: Router) {
  }

  ngOnInit(): void {
    this.startFetchingHistory();
  }

  ngOnDestroy(): void {
    this.stopFetchingHistory();
  }

  average(items: any[]): number {
    let average = 0;
    if (items.length > 0) {
      let sum = 0;
      for (let i = 0; i < items.length; i++) {
        sum += items[i];
      }
      average = sum / items.length;
    }
    return average;
  }

  min(items: any[]): number {
    let min = 0;
    if (items.length > 0) {
      min = Number.MAX_VALUE;
      for (let i = 0; i < items.length; i++) {
        if (items[i] < min) {
          min = items[i];
        }
      }
    }
    return min;
  }

  max(items: any[]): number {
    let max = 0;
    if (items.length > 0) {
      for (let i = 0; i < items.length; i++) {
        if (items[i] > max) {
          max = items[i];
        }
      }
    }
    return max;
  }

  standardDeviation(items: any[]): number {
    let sumOfDifferences = 0;
    const average = this.average(items);
    if (items.length > 0) {
      for (let i = 0; i < items.length; i++) {
        sumOfDifferences += Math.pow((average - items[i]), 2);
      }
    }
    return Math.sqrt(sumOfDifferences / (items.length - 1));
  }

  private startFetchingHistory(): void {
    this.stopFetchingHistory();

    const timeStampTimer = Observable.timer(0, 1000);
    this.timeStampUpdateSubscription = timeStampTimer.subscribe(t => {
      this.elapsedTime = new Date().getTime() - this.startUpTimeStamp;
      this.formattedElapsedTime = this.formatTime(this.elapsedTime);
      this.generateStatistics();
    });

    const historyTimer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    this.historyFetchSubscription = historyTimer.subscribe(t => {
      this.http.get<ServerResponse<History>>(
        Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_HISTORY_PATH
      ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.startUpTimeStamp = response.data.startUpTimeStamp;

          const nodeSerEntries = Object.entries(response.data.nodeSerMessages);
          const serMessages = [];
          for (let i = 0; i < nodeSerEntries.length; i++) {
            const serEntries = Object.entries(nodeSerEntries[i][1]);
            for (let j = 0; j < serEntries.length; j++) {
              serMessages.push([
                nodeSerEntries[i][0],
                parseInt(serEntries[j][0], 10),
                <SerMessage> serEntries[j][1]
              ]);
            }
          }
          this.serMessages = serMessages;
          this.serMessagesDataSource = new TableDataSource<[number, SerMessage]>(serMessages);

          const nodeSerSuperPeerEntries = Object.entries(response.data.nodeSerSuperPeerMessages);
          const serSuperPeerMessages = [];
          for (let i = 0; i < nodeSerSuperPeerEntries.length; i++) {
            const serSuperPeerEntries = Object.entries(nodeSerSuperPeerEntries[i][1]);
            for (let j = 0; j < serSuperPeerEntries.length; j++) {
              serSuperPeerMessages.push([
                nodeSerSuperPeerEntries[i][1],
                parseInt(serSuperPeerEntries[j][0], 10),
                <SerMessage> serSuperPeerEntries[j][1]
              ]);
            }
          }
          this.serSuperPeerMessages = serSuperPeerMessages;
          this.serSuperPeerMessagesDataSource = new TableDataSource<[number, SerSuperPeerMessage]>(serSuperPeerMessages);

          this.bootstrappingMessageCount = response.data.bootstrappingMessageCount;
          this.maintenanceMessageCount = response.data.maintenanceMessageCount;

          this.generateStatistics();
        } else if (response.status === ServerResponseStatus.IN_FILE_SHARER_MODE) {
          this.router.navigateByUrl('/home');
        } else {
          this.startUpTimeStamp = new Date().getTime();
          this.serMessagesDataSource = new TableDataSource<[number, SerMessage]>([]);
          this.serSuperPeerMessagesDataSource = new TableDataSource<[number, SerSuperPeerMessage]>([]);
          this.bootstrappingMessageCount = 0;
          this.maintenanceMessageCount = 0;
        }
      });
    });
  }

  private stopFetchingHistory(): void {
    if (this.historyFetchSubscription) {
      this.historyFetchSubscription.unsubscribe();
      this.historyFetchSubscription = null;
    }
    if (this.timeStampUpdateSubscription) {
      this.timeStampUpdateSubscription.unsubscribe();
      this.timeStampUpdateSubscription = null;
    }
  }

  private formatTime(time: number): string {
    let formattedString = 'System not yet started';
    if (time > 0) {
      // Calculating milliseconds
      const milliseconds = time % 1000;

      // Calculating the seconds
      time = (time - milliseconds) / 1000;
      const seconds = time % 60;

      // Calculating minutes
      time = (time - seconds) / 60;
      const minutes = time % 60;

      // Calculating hours
      const hours = (time - minutes) / 60;

      formattedString = hours + ':' + minutes + ':' + seconds;
    }
    return formattedString;
  }

  private generateStatistics() {
    {
      const statistics = [];
      if (this.formattedElapsedTime) {
        statistics.push(['System Up Time (H:mm:ss)', this.formattedElapsedTime]);
      }
      if (this.nodes) {
        statistics.push(['Total Nodes', this.nodes.length]);
      }
      if (this.bootstrappingMessageCount) {
        statistics.push(['Total Bootstrapping Messages Count', this.bootstrappingMessageCount]);
        if (this.nodes.length > 0) {
          statistics.push(['Bootstrapping Messages Count (per node)',
            (this.bootstrappingMessageCount / this.nodes.length).toFixed(2)]);
        }
      }
      if (this.maintenanceMessageCount && this.elapsedTime) {
        const messagesCount = this.maintenanceMessageCount / (this.elapsedTime / 60000);
        statistics.push(['Maintenance Messages Count (per minute)', messagesCount.toFixed(2)]);
        if (this.nodes.length > 0) {
          statistics.push(['Maintenance Messages Count (per minute per node)', (messagesCount / this.nodes.length).toFixed(2)]);
        }
      }
      this.generalStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }

    if (this.serMessages) {
      const statistics = [];

      const serMessagesHopCounts = [];
      let successfulQueries = 0;
      for (let i = 0; i < this.serMessages.length; i++) {
        const hopCounts = this.serMessages[i][2].hopCounts;
        if (hopCounts.length > 0) {
          successfulQueries++;
          let firstHitHopCount = Number.MAX_VALUE;
          for (let j = 0; j < hopCounts.length; j++) {
            const hopCount = hopCounts[j];
            if (hopCount < firstHitHopCount) {
              firstHitHopCount = hopCount;
            }
          }
          serMessagesHopCounts.push(firstHitHopCount);
        }
      }
      if (this.serMessages.length > 0) {
        statistics.push(['Success Rate', (successfulQueries * 100 / this.serMessages.length).toFixed(2) + '%']);
      }
      if (serMessagesHopCounts.length > 0) {
        statistics.push(['Minimum First Hit Hops (per query)', this.min(serMessagesHopCounts)]);
        statistics.push(['Maximum First Hit Hops (per query)', this.max(serMessagesHopCounts)]);
        statistics.push(['Average First Hit Hops (per query)', this.average(serMessagesHopCounts).toFixed(2)]);
        statistics.push(['Standard Deviation First Hit Hops (per query)', this.standardDeviation(serMessagesHopCounts).toFixed(2)]);
      }

      let serMessagesTotal = 0;
      const serMessagesCount = [];
      for (let i = 0; i < this.serMessages.length; i++) {
        serMessagesCount.push(this.serMessages[i][2].messagesCount);
        serMessagesTotal += this.serMessages[i][2].messagesCount;
      }
      if (serMessagesCount.length > 0) {
        statistics.push(['Minimum Messages (per query)', this.min(serMessagesCount)]);
        statistics.push(['Maximum Messages (per query)', this.max(serMessagesCount)]);
        statistics.push(['Average Messages (per query)', this.average(serMessagesCount).toFixed(2)]);
        statistics.push(['Standard Deviation Messages (per query)', this.standardDeviation(serMessagesCount).toFixed(2)]);
      }
      statistics.push(['Total Messages', serMessagesTotal]);

      const serMessagesDelays = [];
      for (let i = 0; i < this.serMessages.length; i++) {
        serMessagesDelays.push(this.serMessages[i][2].firstHitTimeStamp - this.serMessages[i][2].firstHitTimeStamp);
      }
      if (serMessagesDelays.length > 0) {
        statistics.push(['Minimum Delay (per query)', this.min(serMessagesDelays) + 'milliseconds']);
        statistics.push(['Maximum Delay (per query)', this.max(serMessagesDelays) + 'milliseconds']);
        statistics.push(['Average Delay (per query)', this.average(serMessagesDelays).toFixed(2) + 'milliseconds']);
        statistics.push(['Standard Deviation Delay (per query)', this.standardDeviation(serMessagesDelays).toFixed(2) + 'milliseconds']);
      }

      this.serMessageStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }

    if (this.serSuperPeerMessages) {
      const statistics = [];

      const serSuperPeerMessagesHopCounts = [];
      let successfulQueries = 0;
      for (let i = 0; i < this.serSuperPeerMessages.length; i++) {
        const hopCounts = this.serSuperPeerMessages[i][2].hopCounts;
        if (hopCounts.length > 0) {
          successfulQueries++;
          let firstHitHopCount = Number.MAX_VALUE;
          for (let j = 0; j < hopCounts.length; j++) {
            const hopCount = hopCounts[j];
            if (hopCount < firstHitHopCount) {
              firstHitHopCount = hopCount;
            }
          }
          serSuperPeerMessagesHopCounts.push(firstHitHopCount);
        }
      }
      if (this.serSuperPeerMessages.length > 0) {
        statistics.push(['Success Rate', (successfulQueries * 100 / this.serSuperPeerMessages.length).toFixed(2) + '%']);
      }
      if (serSuperPeerMessagesHopCounts.length > 0) {
        statistics.push(['Minimum First Hit Hops (per query)', this.min(serSuperPeerMessagesHopCounts)]);
        statistics.push(['Maximum First Hit Hops (per query)', this.max(serSuperPeerMessagesHopCounts)]);
        statistics.push(['Average First Hit Hops (per query)', this.average(serSuperPeerMessagesHopCounts).toFixed(2)]);
        statistics.push(['Standard Deviation First Hit Hops (per query)',
          this.standardDeviation(serSuperPeerMessagesHopCounts).toFixed(2)]);
      }

      let serSuperPeerMessagesTotal = 0;
      const serSuperPeerMessagesCounts = [];
      for (let i = 0; i < this.serMessages.length; i++) {
        serSuperPeerMessagesCounts.push(this.serMessages[i][2].messagesCount);
        serSuperPeerMessagesTotal += this.serMessages[i][2].messagesCount;
      }
      if (serSuperPeerMessagesCounts.length > 0) {
        statistics.push(['Minimum Messages (per query)', this.min(serSuperPeerMessagesCounts)]);
        statistics.push(['Maximum Messages (per query)', this.max(serSuperPeerMessagesCounts)]);
        statistics.push(['Average Messages (per query)', this.average(serSuperPeerMessagesCounts).toFixed(2)]);
        statistics.push(['Standard Deviation Messages (per query)',
          this.standardDeviation(serSuperPeerMessagesCounts).toFixed(2)]);
      }
      statistics.push(['Total Messages', serSuperPeerMessagesTotal]);

      const serSuperPeerMessagesDelays = [];
      for (let i = 0; i < this.serSuperPeerMessages.length; i++) {
        serSuperPeerMessagesDelays.push(
          this.serSuperPeerMessages[i][2].firstHitTimeStamp - this.serSuperPeerMessages[i][2].firstHitTimeStamp
        );
      }
      if (serSuperPeerMessagesDelays.length > 0) {
        statistics.push(['Minimum Delay (per query)', this.min(serSuperPeerMessagesDelays) + 'milliseconds']);
        statistics.push(['Maximum Delay (per query)', this.max(serSuperPeerMessagesDelays) + 'milliseconds']);
        statistics.push(['Average Delay (per query)', this.average(serSuperPeerMessagesDelays).toFixed(2) + 'milliseconds']);
        statistics.push(['Standard Deviation Delay (per query)',
          this.standardDeviation(serSuperPeerMessagesDelays).toFixed(2) + 'milliseconds']);
      }

      this.serSuperPeerMessageStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }
  }
}
