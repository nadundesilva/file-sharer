import {DataSource} from '@angular/cdk/collections';
import {Observable} from 'rxjs/Observable';
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
  public static API_SYSTEM_ENDPOINT_SHUTDOWN_PATH = 'shutdown/';
  public static API_SYSTEM_ENDPOINT_MODE_PATH = 'mode/';
  public static API_TRACE_ENDPOINT = 'trace/';
  public static API_TRACE_ENDPOINT_STATE_PATH = 'state/';
  public static API_TRACE_ENDPOINT_NETWORK_PATH = 'network/';
  public static API_TRACE_ENDPOINT_HISTORY_PATH = 'history/';
  public static REFRESH_FREQUENCY = 3000;
}

export enum ServerResponseStatus {
  SUCCESS = <any>'SUCCESS',
  IN_TRACER_MODE = <any>'IN_TRACER_MODE',
  IN_FILE_SHARER_MODE = <any>'IN_FILE_SHARER_MODE',
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

export enum TraceableState {
  TRACEABLE = <any>'TRACEABLE',
  OFF = <any>'OFF'
}

export enum PeerType {
  SUPER_PEER = <any>'SUPER_PEER',
  ORDINARY_PEER = <any>'ORDINARY_PEER'
}

export enum NodeState {
  ACTIVE = <any>'ACTIVE',
  PENDING_INACTIVATION = <any>'PENDING_INACTIVATION',
  INACTIVE = <any>'INACTIVE'
}

export class Node {
  ip: string;
  port: number;
  state: NodeState;
}

export interface ServerResponse<T> {
  status: ServerResponseStatus;
  data: T;
}

export class TraceableNode extends Node {
  peerType: PeerType;
}

export enum ConnectionType {
  MAIN = <any>'MAIN',
  SUB = <any>'SUB'
}

export class NetworkConnection {
  node1: TraceableNode;
  node2: TraceableNode;
  type: ConnectionType;
}

export class Network {
  nodes: TraceableNode[] = [];
  unstructuredNetwork: NetworkConnection[] = [];
  superPeerNetwork: NetworkConnection[] = [];
  assignedSuperPeersNetwork: NetworkConnection[] = [];
}

@Injectable()
export class Utils {
  constructor(private snackBar: MatSnackBar, private http: HttpClient) {
  }

  public showNotification(message: string): void {
    this.snackBar.open(message, 'Close', {duration: 3000});
  }
}

export class TableDataSource<T> extends DataSource<T> {
  constructor(private data: T[]) {
    super();
  }

  connect(): Observable<T[]> {
    return Observable.of(this.data);
  }

  disconnect(): void {

  }

  size(): number {
    return this.data.length;
  }

  isEmpty(): boolean {
    return !this.data || this.data.length === 0;
  }
}
