import {DataSource} from "@angular/cdk/collections";
import {Observable} from "rxjs/Observable";
import {BehaviorSubject} from "rxjs/BehaviorSubject";
import {MatSnackBar} from "@angular/material";
import {Injectable} from "@angular/core";

export class Constants {
  public static API_ENDPOINT = 'api/';
  public static API_QUERY_ENDPOINT = 'query/';
}

@Injectable()
export class Utils {
  constructor(private snackBar: MatSnackBar) {
  }

  public showNotification(message: string) {
    this.snackBar.open(message, "Close", {duration: 3000});
  }
}

export enum ServerResponseStatus {
  SUCCESS = <any>"SUCCESS",
  ERROR = <any>"ERROR"
}

export interface Node {
  ip: string;
  port: number;
  isAlive: boolean;
}

export interface AggregatedResource extends TableDataSourceItem {
  name: string;
  nodes: Node[];
}

export interface ServerResponse<T> {
  status: ServerResponseStatus;
  data: T;
}

export abstract class TableDataSourceItem {
  abstract get toString();
}

export class TableDataSource<T extends TableDataSourceItem> extends DataSource<T> {
  _filterChange = new BehaviorSubject('');
  get filter(): string { return this._filterChange.value; }
  set filter(filter: string) { this._filterChange.next(filter); }

  constructor(private data: T[]) {
    super();
  }

  connect(): Observable<T[]> {
    return Observable.of(this._filterChange).map(() => {
      return this.data.slice().filter((item: T) => {
        let searchStr = item.toString().toLowerCase();
        return searchStr.indexOf(this.filter.toLowerCase()) != -1;
      });
    });
  }

  disconnect() {
  }

  size() {
    return this.data.length;
  }
}
