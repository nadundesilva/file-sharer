package org.microfuse.node.commons;

import org.microfuse.node.core.communication.network.NetworkHandlerType;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.node.core.utils.Constants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private int nodeID;
    private String address;
    private int startingTimeToLive;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;
    private int tcpListeningPort;

    public Configuration() {
        nodeID = -1;
        address = null;
        startingTimeToLive = Constants.DEFAULT_STARTING_TIME_TO_LIVE;
        networkHandlerType = Constants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = Constants.DEFAULT_ROUTING_STRATEGY;
        tcpListeningPort = 4444;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getStartingTimeToLive() {
        return startingTimeToLive;
    }

    public void setStartingTimeToLive(int startingTimeToLive) {
        this.startingTimeToLive = startingTimeToLive;
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

    public int getTcpListeningPort() {
        return tcpListeningPort;
    }

    public void setTcpListeningPort(int tcpListeningPort) {
        this.tcpListeningPort = tcpListeningPort;
    }
}
