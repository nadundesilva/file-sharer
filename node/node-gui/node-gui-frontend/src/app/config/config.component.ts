import {Component, OnInit} from '@angular/core';
import {
  Constants, NetworkHandlerType, RoutingStrategyType, ServerResponse, ServerResponseStatus,
  Utils
} from '../commons';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html'
})
export class ConfigComponent implements OnInit {
  title = 'Configuration';

  config: Config;
  networkHandlerType = NetworkHandlerType;
  routingStrategyType = RoutingStrategyType;

  constructor(private http: HttpClient, private utils: Utils) { }

  ngOnInit(): void {
    this.fetchConfiguration();
  }

  fetchConfiguration(): void {
    this.http.get<ServerResponse<Config>>(Constants.API_ENDPOINT + Constants.API_CONFIG_ENDPOINT)
      .subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.setConfig(response.data);
          this.utils.showNotification('Successfully loaded the configuration.');
        } else {
          this.setConfig(null);
          this.utils.showNotification('Failed to load the configuration.');
        }
      });
  }

  loadDefaults(): void {
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_CONFIG_ENDPOINT + Constants.API_CONFIG_ENDPOINT_DEFAULTS_PATH, {}
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Successfully loaded the configuration default values. ' +
          'The new configuration will take effect in a moment.');
        this.setConfig(response.data);
      } else {
        this.utils.showNotification('Failed to load configuration default values.');
      }
    });
  }

  save(): void {
    const config = this.getConfig();
    this.http.post<ServerResponse<any>>(
      Constants.API_ENDPOINT + Constants.API_CONFIG_ENDPOINT, config
    ).subscribe(response => {
      if (response.status === ServerResponseStatus.SUCCESS) {
        this.utils.showNotification('Successfully saved the configuration. ' +
          'The new configuration will take effect in a moment.');
      } else {
        this.utils.showNotification('Failed to save the configuration');
      }
    });
  }

  private setConfig(config: Config): void {
    if (config != null) {
      this.config = Object.assign({}, config);
      this.config.bootstrapServerReplyWaitTimeout = this.config.bootstrapServerReplyWaitTimeout / 1000;
      this.config.networkHandlerSendTimeout = this.config.networkHandlerSendTimeout / 1000;
      this.config.serSuperPeerTimeout = this.config.serSuperPeerTimeout / 1000;
      this.config.heartbeatInterval = this.config.heartbeatInterval / 1000;
      this.config.gossipingInterval = this.config.gossipingInterval / 1000;
      this.config.automatedGarbageCollectionInterval = this.config.automatedGarbageCollectionInterval / 1000;
    }
  }

  private getConfig(): Config {
    const config = Object.assign({}, this.config);
    config.bootstrapServerReplyWaitTimeout = config.bootstrapServerReplyWaitTimeout * 1000;
    config.networkHandlerSendTimeout = config.networkHandlerSendTimeout * 1000;
    config.serSuperPeerTimeout = config.serSuperPeerTimeout * 1000;
    config.heartbeatInterval = config.heartbeatInterval * 1000;
    config.gossipingInterval = config.gossipingInterval * 1000;
    config.automatedGarbageCollectionInterval = config.automatedGarbageCollectionInterval * 1000;
    return config;
  }
}

class Config {
  bootstrapServerIP: string;
  bootstrapServerPort: number;
  username: string;
  tracerServeIP: string;
  tracerServePort: number;
  ip: string;
  peerListeningPort: number;
  networkHandlerThreadCount: number;
  timeToLive: number;
  maxAssignedOrdinaryPeerCount: number;
  maxUnstructuredPeerCount: number;
  heartbeatInterval: number;
  gossipingInterval: number;
  networkHandlerSendTimeout: number;
  rmiRegistryEntryPrefix: string;
  bootstrapServerReplyWaitTimeout: number;
  serSuperPeerTimeout: number;
  automatedGarbageCollectionInterval: number;
  udpNetworkHandlerRetryInterval: number;
  udpNetworkHandlerRetryCount: number;
  networkHandlerType: NetworkHandlerType;
  routingStrategyType: RoutingStrategyType;
}
