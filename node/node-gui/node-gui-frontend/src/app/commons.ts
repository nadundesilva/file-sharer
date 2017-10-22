import {DataSource} from "@angular/cdk/collections";
import {Observable} from "rxjs/Observable";
import {BehaviorSubject} from "rxjs/BehaviorSubject";
import {MatSnackBar} from "@angular/material";
import {Injectable} from "@angular/core";

export class Constants {
  public static API_ENDPOINT = 'api/';
  public static API_QUERY_ENDPOINT = 'query/';
  public static API_NETWORK_ENDPOINT = 'network/';
  public static REFRESH_FREQUENCY = 5000;
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

export class TableDataSource<T extends TableDataSourceItem> extends DataSource<T> {
  _filterChange = new BehaviorSubject('');

  get filter(): string {
    return this._filterChange.value;
  }

  set filter(filter: string) {
    this._filterChange.next(filter);
  }

  constructor(private data: T[]) {
    super();
  }

  connect(): Observable<T[]> {
    // return Observable.of(this._filterChange).map(() => {
    //   return this.data.slice().filter((item: T) => {
    //     let searchStr = item.filterString().toLowerCase();
    //     return searchStr.indexOf(this.filter.toLowerCase()) != -1;
    //   });
    // });
    return Observable.of(this.data);
  }

  disconnect() {
  }

  isEmpty() {
    return !this.data || this.data.length == 0;
  }
}

export abstract class TableDataSourceItem {
  abstract get filterString();
}

export enum PeerType {
  SUPER_PEER = <any>"Super Peer",
  ORDINARY_PEER = <any>"Ordinary Peer"
}

export class Node extends TableDataSourceItem {
  ip: string;
  port: number;
  isAlive: boolean;

  get filterString(): string {
    return this.ip + ":" + this.port;
  }
}

export interface ServerResponse<T> {
  status: ServerResponseStatus;
  data: T;
}
