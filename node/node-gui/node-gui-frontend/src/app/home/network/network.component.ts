import {Component, OnDestroy, OnInit} from '@angular/core';
import {Constants, PeerType, ServerResponse, ServerResponseStatus, TableDataSource, Node} from '../../commons';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';
import {Subscription} from 'rxjs/Subscription';

class NodeInfo {
  peerType: PeerType;
  unstructuredNetwork: TableDataSource<Node>;

  // Only in super peers
  superPeerNetwork: TableDataSource<Node>;
  assignedOrdinaryPeers: TableDataSource<Node>;

  // Only in ordinary peers
  assignedSuperPeer: Node;
}

@Component({
  selector: 'app-network',
  templateUrl: './network.component.html'
})
export class NetworkComponent implements OnInit, OnDestroy {
  displayedColumns = ['node-ip', 'node-port'];
  peerType = PeerType;

  nodeInfo: NodeInfo;
  nodeInfoFetchSubscription: Subscription;

  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.startFetchingNodeInfo();
  }

  ngOnDestroy(): void {
    this.stopFetchingNodeInfo();
  }

  private startFetchingNodeInfo(): void {
    this.stopFetchingNodeInfo();
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    this.nodeInfoFetchSubscription = timer.subscribe(t => {
      this.http.get<ServerResponse<any>>(Constants.API_ENDPOINT + Constants.API_NETWORK_ENDPOINT)
        .subscribe(response => {
          if (response.status === ServerResponseStatus.SUCCESS) {
            this.nodeInfo = new NodeInfo();
            this.nodeInfo.peerType = response.data.peerType;
            this.nodeInfo.unstructuredNetwork = new TableDataSource<Node>(response.data.unstructuredNetwork);
            console.log(this.nodeInfo.peerType === PeerType.SUPER_PEER);
            if (this.nodeInfo.peerType === PeerType.SUPER_PEER) {
              this.nodeInfo.superPeerNetwork = new TableDataSource<Node>(response.data.superPeerNetwork);
              this.nodeInfo.assignedOrdinaryPeers = new TableDataSource<Node>(response.data.assignedOrdinaryPeers);
            } else {
              if (response.data.assignedSuperPeer) {
                this.nodeInfo.assignedSuperPeer = response.data.assignedSuperPeer;
              }
            }
          } else {
            this.nodeInfo = null;
          }
        });
    });
  }

  private stopFetchingNodeInfo(): void {
    if (this.nodeInfoFetchSubscription) {
      this.nodeInfoFetchSubscription.unsubscribe();
      this.nodeInfoFetchSubscription = null;
    }
  }
}
