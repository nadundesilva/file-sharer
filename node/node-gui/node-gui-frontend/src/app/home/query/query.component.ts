import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TableDataSource, Utils} from '../../commons';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html'
})
export class QueryComponent implements OnInit, OnDestroy {
  queryString = '';

  runningQueries: string[];
  runningQueriesFetchSubscription: Subscription;

  selectedRunningQuery: string;
  selectedRunningQueryFetchSubscription: Subscription;

  queryResults: TableDataSource<AggregatedResource>;
  displayedColumns = ['name', 'node-ip', 'node-port'];

  @ViewChild('filter') filterElement: ElementRef;

  constructor(private http: HttpClient, private utils: Utils) {
  }

  ngOnInit(): void {
    this.startFetchingRunningQueries();
    this.startFetchingQueryResults();
  }

  ngOnDestroy(): void {
    this.stopFetchingRunningQueries();
    this.stopFetchingQueryResults();
  }

  query(): void {
    const queryString = this.queryString;
    if (queryString) {
      this.http.post<ServerResponse<any>>(
        Constants.API_ENDPOINT + Constants.API_QUERY_ENDPOINT + queryString, {}
      ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.utils.showNotification('Successfully started search ' + queryString);
          this.fetchRunningQueries();
        } else {
          this.utils.showNotification('Error in starting search ' + queryString);
        }
      });
    } else {
      this.utils.showNotification('Cannot search with empty string');
    }
  }

  clearResources(): void {
    this.http.delete<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_QUERY_ENDPOINT
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Successfully cleared search results');
        this.fetchRunningQueries();
      } else {
        this.utils.showNotification('Error in clearing search results');
      }
    });
  }

  private startFetchingRunningQueries(): void {
    this.stopFetchingRunningQueries();
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    this.runningQueriesFetchSubscription = timer.subscribe(t => this.fetchRunningQueries());
  }

  private stopFetchingRunningQueries(): void {
    if (this.runningQueriesFetchSubscription) {
      this.runningQueriesFetchSubscription.unsubscribe();
      this.runningQueriesFetchSubscription = null;
    }
  }

  private fetchRunningQueries(): void {
    this.http.get<ServerResponse<string[]>>(Constants.API_ENDPOINT + Constants.API_QUERY_ENDPOINT)
      .subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.runningQueries = response.data;
        } else {
          this.runningQueries = [];
        }
      });
  }

  private startFetchingQueryResults(): void {
    this.stopFetchingQueryResults();
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    this.selectedRunningQueryFetchSubscription = timer.subscribe(t => this.fetchQueryResults());
  }

  private stopFetchingQueryResults(): void {
    if (this.selectedRunningQueryFetchSubscription) {
      this.selectedRunningQueryFetchSubscription.unsubscribe();
      this.selectedRunningQueryFetchSubscription = null;
    }
  }

  fetchQueryResults(): void {
    if (this.selectedRunningQuery) {
      this.http.get<ServerResponse<AggregatedResource[]>>(
        Constants.API_ENDPOINT + Constants.API_QUERY_ENDPOINT + this.selectedRunningQuery
      ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.queryResults = new TableDataSource<AggregatedResource>(response.data);
        } else {
          this.queryResults = new TableDataSource<AggregatedResource>([]);
        }
      });
    } else {
      this.queryResults = new TableDataSource<AggregatedResource>([]);
    }
  }
}

class AggregatedResource {
  name: string;
  nodes: Node[];
}
