import {Component, OnInit} from '@angular/core';
import {
  Constants, NetworkHandlerType, RoutingStrategyType, ServerResponse, ServerResponseStatus,
  Utils
} from '../commons';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html'
})
export class ConfigComponent implements OnInit {
  title = 'Configuration';

  config: Config;
  networkHandlerType = NetworkHandlerType;
  routingStrategyType = RoutingStrategyType;

  constructor(private http: HttpClient, private router: Router, private utils: Utils) { }

  ngOnInit(): void {
    this.fetchConfiguration();
  }

  fetchConfiguration(): void {
    this.http.get<ServerResponse<Config>>(Constants.API_ENDPOINT + Constants.API_CONFIG_ENDPOINT)
      .subscribe(response => {
        if (response.status === ServerResponseStatus.SUCCESS) {
          this.setConfig(response.data);
          this.utils.showNotification('Successfully loaded the configuration.');
        } else if (response.status === ServerResponseStatus.IN_TRACER_MODE) {
          this.router.navigateByUrl('/tracer');
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
      } else if (response.status === ServerResponseStatus.IN_TRACER_MODE) {
        this.router.navigateByUrl('/tracer');
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
      } else if (response.status === ServerResponseStatus.IN_TRACER_MODE) {
        this.router.navigateByUrl('/tracer');
      } else {
        this.utils.showNotification('Failed to save the configuration');
      }
    });
  }

  private setConfig(config: Config): void {
    if (config != null) {
      this.config = Object.assign({}, config);
      this.config.heartbeatInterval = this.config.heartbeatInterval / 1000;
      this.config.gossipingInterval = this.config.gossipingInterval / 1000;
      this.config.bootstrapServerReplyWaitTimeout = this.config.bootstrapServerReplyWaitTimeout / 1000;
      this.config.serSuperPeerTimeout = this.config.serSuperPeerTimeout / 1000;
      this.config.automatedGarbageCollectionInterval = this.config.automatedGarbageCollectionInterval / 1000;
      this.config.udpNetworkHandlerRetryInterval = this.config.udpNetworkHandlerRetryInterval / 1000;
    }
  }

  private getConfig(): Config {
    const config = Object.assign({}, this.config);
    config.heartbeatInterval = config.heartbeatInterval * 1000;
    config.gossipingInterval = config.gossipingInterval * 1000;
    config.bootstrapServerReplyWaitTimeout = config.bootstrapServerReplyWaitTimeout * 1000;
    config.serSuperPeerTimeout = config.serSuperPeerTimeout * 1000;
    config.automatedGarbageCollectionInterval = config.automatedGarbageCollectionInterval * 1000;
    config.udpNetworkHandlerRetryInterval = config.udpNetworkHandlerRetryInterval * 1000;
    return config;
  }
}

class Config {
  bootstrapServerIP: string;
  bootstrapServerPort: number;
  usernamePrefix: string;
  ip: string;
  peerListeningPort: number;
  networkHandlerThreadCount: number;
  rmiRegistryEntryPrefix: string;
  timeToLive: number;
  maxAssignedOrdinaryPeerCount: number;
  maxUnstructuredPeerCount: number;
  maxSuperPeerCount: number;
  heartbeatInterval: number;
  gossipingInterval: number;
  bootstrapServerReplyWaitTimeout: number;
  serSuperPeerTimeout: number;
  automatedGarbageCollectionInterval: number;
  udpNetworkHandlerRetryInterval: number;
  udpNetworkHandlerRetryCount: number;
  networkHandlerType: NetworkHandlerType;
  routingStrategyType: RoutingStrategyType;
}
