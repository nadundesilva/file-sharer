package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.utils.Constants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private String address;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;
    private PeerType peerType;
    private int peerListeningPort;
    private int timeToLive;

    public Configuration() {
        address = null;
        networkHandlerType = Constants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = Constants.DEFAULT_ROUTING_STRATEGY;
        peerType = Constants.DEFAULT_PEER_TYPE;
        peerListeningPort = Constants.DEFAULT_TCP_LISTENER_PORT;
        timeToLive = Constants.DEFAULT_TIME_TO_LIVE;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public PeerType getPeerType() {
        return peerType;
    }

    public void setPeerType(PeerType peerType) {
        this.peerType = peerType;
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
