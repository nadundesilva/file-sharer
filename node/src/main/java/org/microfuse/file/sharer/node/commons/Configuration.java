package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.utils.Constants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private String ip;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;
    private int peerListeningPort;
    private int timeToLive;

    public Configuration() {
        ip = Constants.DEFAULT_IP_ADDRESS;
        networkHandlerType = Constants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = Constants.DEFAULT_ROUTING_STRATEGY;
        peerListeningPort = Constants.DEFAULT_TCP_LISTENER_PORT;
        timeToLive = Constants.DEFAULT_TIME_TO_LIVE;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public NetworkHandlerType getNetworkHandlerType() {
        return networkHandlerType;
    }

    public void setNetworkHandlerType(NetworkHandlerType networkHandlerType) {
        this.networkHandlerType = networkHandlerType;
    }

    public RoutingStrategyType getRoutingStrategyType() {
        return routingStrategyType;
    }

    public void setRoutingStrategyType(RoutingStrategyType routingStrategyType) {
        this.routingStrategyType = routingStrategyType;
    }

    public int getPeerListeningPort() {
        return peerListeningPort;
    }

    public void setPeerListeningPort(int peerListeningPort) {
        this.peerListeningPort = peerListeningPort;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }
}
