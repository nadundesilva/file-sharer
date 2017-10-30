import {DataSource} from '@angular/cdk/collections';
import {Observable} from 'rxjs/Observable';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {MatSnackBar} from '@angular/material';
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import 'rxjs/add/observable/of';

export class Constants {
  public static API_ENDPOINT = 'api/';
  public static API_QUERY_ENDPOINT = 'query/';
  public static API_NETWORK_ENDPOINT = 'network/';
  public static API_RESOURCES_ENDPOINT = 'resources/';
  public static API_CONFIG_ENDPOINT = 'config/';
  public static API_CONFIG_ENDPOINT_DEFAULTS_PATH = 'defaults/';
  public static API_SYSTEM_ENDPOINT = 'system/';
  public static API_SYSTEM_ENDPOINT_RESTART_PATH = 'restart/';
  public static REFRESH_FREQUENCY = 3000;
}

export enum ServerResponseStatus {
  SUCCESS = <any>'SUCCESS',
  ERROR = <any>'ERROR'
}

export enum NetworkHandlerType {
  WEB_SERVICES = <any>'WEB_SERVICES',
  RMI = <any>'RMI',
  TCP_SOCKET = <any>'TCP_SOCKET',
  UDP_SOCKET = <any>'UDP_SOCKET'
}

export enum RoutingStrategyType {
  UNSTRUCTURED_FLOODING = <any>'UNSTRUCTURED_FLOODING',
  UNSTRUCTURED_RANDOM_WALK = <any>'UNSTRUCTURED_RANDOM_WALK',
  SUPER_PEER_FLOODING = <any>'SUPER_PEER_FLOODING',
  SUPER_PEER_RANDOM_WALK = <any>'SUPER_PEER_RANDOM_WALK'
}

export enum PeerType {
  SUPER_PEER = <any>'Super Peer',
  ORDINARY_PEER = <any>'Ordinary Peer'
}

export class Node {
  ip: string;
  port: number;
  isAlive: boolean;
}

export interface ServerResponse<T> {
  status: ServerResponseStatus;
  data: T;
}

@Injectable()
export class Utils {
  constructor(private snackBar: MatSnackBar, private http: HttpClient) {
  }

  public showNotification(message: string): void {
    this.snackBar.open(message, 'Close', {duration: 3000});
  }

  public restart() {
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_SYSTEM_ENDPOINT + Constants.API_SYSTEM_ENDPOINT_RESTART_PATH, {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.showNotification('Restarting server');
      } else {
        this.showNotification('Failed to restart server');
      }
    });
  }
}

export class TableDataSource<T> extends DataSource<T> {
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
    return Observable.of(this.data);
  }

  disconnect(): void {

  }

  isEmpty(): boolean {
    return !this.data || this.data.length === 0;
  }
}
