import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {
  Constants, NodeState, PeerType, ServerResponse, ServerResponseStatus, TableDataSource,
  TraceableNode
} from '../../commons';
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
  bootstrappingMessageCounts: Map<TraceableNode, number>;
  maintenanceMessageCounts: Map<TraceableNode, number>;
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
  bootstrappingMessageCounts: [TraceableNode, number][];
  maintenanceMessageCounts: [TraceableNode, number][];

  serDisplayedColumns = [
    'node', 'sequence-number', 'query', 'messages-count', 'min-hop-count', 'max-hop-count', 'average-hop-count',
    'standard-deviation'
  ];
  serSuperPeerDisplayedColumns = [
    'node', 'sequence-number', 'messages-count', 'min-hop-count', 'max-hop-count', 'average-hop-count',
    'standard-deviation'
  ];
  generalStatisticsColumns = ['name', 'value'];

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

          const nodeSerMessages = Object.entries(response.data.nodeSerMessages);
          const serMessages = [];
          for (let i = 0; i < nodeSerMessages.length; i++) {
            const serMessageEntry = Object.entries(nodeSerMessages[i][1]);
            for (let j = 0; j < serMessageEntry.length; j++) {
              serMessages.push([
                this.getNodeFromList(this.nodes, nodeSerMessages[i][0]),
                parseInt(serMessageEntry[j][0], 10),
                serMessageEntry[j][1]
              ]);
            }
          }
          this.serMessages = serMessages;
          this.serMessagesDataSource = new TableDataSource<[number, SerMessage]>(serMessages);

          const nodeSerSuperPeerMessages = Object.entries(response.data.nodeSerSuperPeerMessages);
          const serSuperPeerMessages = [];
          for (let i = 0; i < nodeSerSuperPeerMessages.length; i++) {
            const serSuperPeerMessageEntry = Object.entries(nodeSerSuperPeerMessages[i][1]);
            for (let j = 0; j < serSuperPeerMessageEntry.length; j++) {
              serMessages.push([
                this.getNodeFromList(this.nodes, nodeSerSuperPeerMessages[i][0]),
                parseInt(serSuperPeerMessageEntry[j][0], 10),
                serSuperPeerMessageEntry[j][1]
              ]);
            }
          }
          this.serSuperPeerMessages = serSuperPeerMessages;
          this.serSuperPeerMessagesDataSource = new TableDataSource<[number, SerSuperPeerMessage]>(serSuperPeerMessages);

          const nodeBootstrappingMessageCounts = Object.entries(response.data.bootstrappingMessageCounts);
          const bootstrappingMessageCounts = [];
          for (let i = 0; i < nodeBootstrappingMessageCounts.length; i++) {
            bootstrappingMessageCounts.push([
              this.getNodeFromList(this.nodes, nodeBootstrappingMessageCounts[i][0]),
              nodeBootstrappingMessageCounts[i][1]
            ]);
          }
          this.bootstrappingMessageCounts = bootstrappingMessageCounts;

          const nodeMaintenanceMessageCounts = Object.entries(response.data.maintenanceMessageCounts);
          const maintenanceMessageCounts = [];
          for (let i = 0; i < nodeMaintenanceMessageCounts.length; i++) {
            maintenanceMessageCounts.push([
              this.getNodeFromList(this.nodes, nodeMaintenanceMessageCounts[i][0]),
              nodeMaintenanceMessageCounts[i][1]
            ]);
          }
          this.maintenanceMessageCounts = maintenanceMessageCounts;

          this.generateStatistics();
        } else if (response.status === ServerResponseStatus.IN_FILE_SHARER_MODE) {
          this.router.navigateByUrl('/home');
        } else {
          this.startUpTimeStamp = new Date().getTime();
          this.serMessagesDataSource = new TableDataSource<[number, SerMessage]>([]);
          this.serSuperPeerMessagesDataSource = new TableDataSource<[number, SerSuperPeerMessage]>([]);
          this.serMessages = [];
          this.serSuperPeerMessages = [];
          this.bootstrappingMessageCounts = [];
          this.maintenanceMessageCounts = [];
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

  private getNodeFromList(nodes: TraceableNode[], address: string) {
    const addressTokens = address.split(':');
    const ip = addressTokens[0];
    const port = parseInt(addressTokens[1], 10);

    let node: TraceableNode;
    for (let i = 0; i > nodes.length; i++) {
      if ((nodes[i].ip === ip) && (nodes[i].port === port)) {
        node = nodes[i];
        break;
      }
    }
    if (!node) {
      node = new TraceableNode();
      node.ip = ip;
      node.port = port;
      node.state = NodeState.ACTIVE;
      node.peerType = PeerType.ORDINARY_PEER;
    }
    return node;
  }

  private generateStatistics() {
    const activeNodes = [];
    if (this.nodes) {
      for (let i = 0; i < this.nodes.length; i++) {
        if (this.nodes[i].state !== NodeState.INACTIVE) {
          activeNodes.push(this.nodes[i]);
        }
      }
    }

    let serMessagesTotal = 0;
    if (this.serMessages) {
      const statistics = [];

      const activeSerMessages = [];
      for (let i = 0; i < this.serMessages.length; i++) {
        activeSerMessages.push(this.serMessages[i]);
      }

      const serMessagesHopCounts = [];
      let successfulQueries = 0;
      for (let i = 0; i < activeSerMessages.length; i++) {
        const hopCounts = activeSerMessages[i][2].hopCounts;
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
      if (activeSerMessages.length > 0) {
        statistics.push(['Success Rate', (successfulQueries * 100 / activeSerMessages.length).toFixed(2) + '%']);
      }
      if (serMessagesHopCounts.length > 0) {
        statistics.push([
          'First Hit Hops (per query)',
          this.min(serMessagesHopCounts),
          this.max(serMessagesHopCounts),
          this.average(serMessagesHopCounts).toFixed(2),
          this.standardDeviation(serMessagesHopCounts).toFixed(2)
        ]);
      }

      const serMessagesCount = [];
      for (let i = 0; i < activeSerMessages.length; i++) {
        serMessagesCount.push(activeSerMessages[i][2].messagesCount);
        serMessagesTotal += activeSerMessages[i][2].messagesCount;
      }
      if (serMessagesCount.length > 0) {
        statistics.push([
          'Messages Count (per query)',
          this.min(serMessagesCount),
          this.max(serMessagesCount),
          this.average(serMessagesCount).toFixed(2),
          this.standardDeviation(serMessagesCount).toFixed(2)
        ]);
      }

      const serMessagesDelays = [];
      for (let i = 0; i < activeSerMessages.length; i++) {
        if (activeSerMessages[i][2].hopCounts.length > 0) {
          serMessagesDelays.push(activeSerMessages[i][2].firstHitTimeStamp - activeSerMessages[i][2].startTimeStamp);
        }
      }
      if (serMessagesDelays.length > 0) {
        statistics.push([
          'Delay (milliseconds per query)',
          this.min(serMessagesDelays),
          this.max(serMessagesDelays),
          this.average(serMessagesDelays).toFixed(2),
          this.standardDeviation(serMessagesDelays).toFixed(2)
        ]);
      }

      this.serMessageStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }

    let serSuperPeerMessagesTotal = 0;
    if (this.serSuperPeerMessages) {
      const statistics = [];

      const activeSerSuperPeerMessages = [];
      for (let i = 0; i < this.serSuperPeerMessages.length; i++) {
        activeSerSuperPeerMessages.push(this.serSuperPeerMessages[i]);
      }

      const serSuperPeerMessagesHopCounts = [];
      let successfulQueries = 0;
      for (let i = 0; i < activeSerSuperPeerMessages.length; i++) {
        const hopCounts = activeSerSuperPeerMessages[i][2].hopCounts;
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
      if (activeSerSuperPeerMessages.length > 0) {
        statistics.push(['Success Rate', (successfulQueries * 100 / activeSerSuperPeerMessages.length).toFixed(2) + '%']);
      }
      if (serSuperPeerMessagesHopCounts.length > 0) {
        statistics.push([
          'First Hit Hops (per query)',
          this.min(serSuperPeerMessagesHopCounts),
          this.max(serSuperPeerMessagesHopCounts),
          this.average(serSuperPeerMessagesHopCounts).toFixed(2),
          this.standardDeviation(serSuperPeerMessagesHopCounts).toFixed(2)
        ]);
      }

      const serSuperPeerMessagesCounts = [];
      for (let i = 0; i < activeSerSuperPeerMessages.length; i++) {
        serSuperPeerMessagesCounts.push(activeSerSuperPeerMessages[i][2].messagesCount);
        serSuperPeerMessagesTotal += activeSerSuperPeerMessages[i][2].messagesCount;
      }
      if (serSuperPeerMessagesCounts.length > 0) {
        statistics.push([
          'Messages Count (per query)',
          this.min(serSuperPeerMessagesCounts),
          this.max(serSuperPeerMessagesCounts),
          this.average(serSuperPeerMessagesCounts).toFixed(2),
          this.standardDeviation(serSuperPeerMessagesCounts).toFixed(2)
        ]);
      }

      const serSuperPeerMessagesDelays = [];
      for (let i = 0; i < activeSerSuperPeerMessages.length; i++) {
        if (activeSerSuperPeerMessages[i][2].hopCounts.length > 0) {
          serSuperPeerMessagesDelays.push(
            activeSerSuperPeerMessages[i][2].firstHitTimeStamp - activeSerSuperPeerMessages[i][2].startTimeStamp
          );
        }
      }
      if (serSuperPeerMessagesDelays.length > 0) {
        statistics.push([
          'Delay (milliseconds per query)',
          this.min(serSuperPeerMessagesDelays),
          this.max(serSuperPeerMessagesDelays),
          this.average(serSuperPeerMessagesDelays).toFixed(2),
          this.standardDeviation(serSuperPeerMessagesDelays).toFixed(2)
        ]);
      }

      this.serSuperPeerMessageStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }

    {
      const statistics = [];
      if (this.formattedElapsedTime) {
        statistics.push(['System Up Time (H:mm:ss)', this.formattedElapsedTime]);
      }
      if (this.nodes) {
        statistics.push(['Total Nodes', this.nodes.length]);
      }
      statistics.push(['Total Active Nodes', activeNodes.length]);
      if (this.bootstrappingMessageCounts) {
        let activeBootstrappingMessageCounts = 0;
        for (let i = 0; i < this.bootstrappingMessageCounts.length; i++) {
          activeBootstrappingMessageCounts += this.bootstrappingMessageCounts[i][1];
        }

        statistics.push(['Total Bootstrapping Messages Count', activeBootstrappingMessageCounts]);
        if (activeNodes.length > 0) {
          statistics.push(['Bootstrapping Messages Count (per node)',
            (activeBootstrappingMessageCounts / activeNodes.length).toFixed(2)]);
        }
      }
      if (this.maintenanceMessageCounts && this.elapsedTime) {
        let activeMaintenanceMessageCounts = 0;
        for (let i = 0; i < this.maintenanceMessageCounts.length; i++) {
          activeMaintenanceMessageCounts += this.maintenanceMessageCounts[i][1];
        }

        const messagesCount = activeMaintenanceMessageCounts / (this.elapsedTime / 60000);
        statistics.push(['Total Maintenance Messages Count (per minute)', messagesCount.toFixed(2)]);
        if (activeNodes.length > 0) {
          statistics.push(['Maintenance Messages Count (per minute per node)', (messagesCount / activeNodes.length).toFixed(2)]);
        }
      }
      statistics.push(['Total Search Messages', serMessagesTotal]);
      statistics.push(['Total Search for Super Peer Messages', serSuperPeerMessagesTotal]);
      this.generalStatisticsDataSource = new TableDataSource<[string, string]>(statistics);
    }
  }
}
