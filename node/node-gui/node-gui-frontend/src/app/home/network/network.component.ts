import {Component, OnInit} from '@angular/core';
import {Constants, PeerType, ServerResponse, ServerResponseStatus, TableDataSource, Node} from '../../commons';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/timer';

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
  templateUrl: './network.component.html',
  styleUrls: ['./network.component.css']
})
export class NetworkComponent implements OnInit {
  displayedColumns = ['node-ip', 'node-port'];
  peerType = PeerType;

  nodeInfo: NodeInfo;

  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.startFetchingNodeInfo();
  }

  startFetchingNodeInfo(): void {
    const timer = Observable.timer(0, Constants.REFRESH_FREQUENCY);
    timer.subscribe(t => {
      this.http.get<ServerResponse<any>>(Constants.API_ENDPOINT + Constants.API_NETWORK_ENDPOINT)
        .subscribe(response => {
          if (response.status === ServerResponseStatus.SUCCESS) {
            this.nodeInfo = new NodeInfo();
            this.nodeInfo.peerType = response.data.peerType;
            this.nodeInfo.unstructuredNetwork = new TableDataSource<Node>(response.data.unstructuredNetwork);
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
}
