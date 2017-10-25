import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Constants, ServerResponse, ServerResponseStatus, TableDataSource, Utils} from '../../commons';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';

@Component({
  selector: 'app-query',
  templateUrl: './query.component.html',
  styleUrls: ['./query.component.css']
})
export class QueryComponent implements OnInit {
  queryString = '';

  runningQueries: string[];
  selectedRunningQuery: string;

  queryResults: TableDataSource<AggregatedResource>;
  displayedColumns = ['name', 'node-ip', 'node-port'];

  @ViewChild('filter') filterElement: ElementRef;

  constructor(private http: HttpClient, private utils: Utils) {
  }

  ngOnInit(): void {
    this.startFetchingRunningQueries();
    this.startFetchingQueryResults();
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

  private startFetchingRunningQueries(): void {
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    timer.subscribe(this.fetchRunningQueries);
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
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    timer.subscribe(this.fetchQueryResults);
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
