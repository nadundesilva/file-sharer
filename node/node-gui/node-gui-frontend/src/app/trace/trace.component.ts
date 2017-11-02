import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  Constants, ServerResponseStatus, TracingMode, ServerResponse, Utils, Network,
  NetworkConnection, ConnectionType
} from '../commons';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-trace',
  templateUrl: './trace.component.html'
})
export class TraceComponent implements OnInit, OnDestroy {
  title = 'Tracing';

  network: Network;
  networkDetailsFetchSubscription: Subscription;

  constructor(private http: HttpClient, private utils: Utils) {
    this.network = new Network();
  }

  ngOnInit(): void {
    this.changeTracingMode(TracingMode.TRACER);
    this.startFetchingNetworkDetails();
  }

  ngOnDestroy(): void {
    this.changeTracingMode(TracingMode.OFF);
    this.stopFetchingNetworkDetails();
  }

  private changeTracingMode(mode: TracingMode) {
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_MODE_PATH + mode,
      {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Changed tracing mode to ' + mode);
      } else {
        this.utils.showNotification('Failed to change tracing mode to ' + mode);
      }
    });
  }

  private startFetchingNetworkDetails(): void {
    this.stopFetchingNetworkDetails();
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    this.networkDetailsFetchSubscription = timer.subscribe(t => {
      this.http.get<ServerResponse<Network>>(
        Constants.API_ENDPOINT + Constants.API_TRACE_ENDPOINT + Constants.API_TRACE_ENDPOINT_NETWORK_PATH
      ).subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.network.unstructuredNetwork = response.data.unstructuredNetwork;
          this.network.superPeerNetwork = response.data.superPeerNetwork;
          this.network.assignedSuperPeersNetwork = response.data.assignedSuperPeersNetwork;

          this.markConnections(this.network.unstructuredNetwork, ConnectionType.SUB);
          this.markConnections(this.network.superPeerNetwork, ConnectionType.MAIN);
          this.markConnections(this.network.assignedSuperPeersNetwork, ConnectionType.SUB);
        } else {
          this.network.unstructuredNetwork = [];
          this.network.superPeerNetwork = [];
          this.network.assignedSuperPeersNetwork = [];
        }
      });
    });
  }

  private stopFetchingNetworkDetails(): void {
    if (this.networkDetailsFetchSubscription) {
      this.networkDetailsFetchSubscription.unsubscribe();
      this.networkDetailsFetchSubscription = null;
    }
  }

  private markConnections(connections: NetworkConnection[], type: ConnectionType) {
    for (let i = 0; i < connections.length; i++) {
      connections[i].type = type;
    }
  }
}
