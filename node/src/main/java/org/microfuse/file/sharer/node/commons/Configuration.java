package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.utils.Constants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private int nodeID;
    private String address;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;
    private int tcpListeningPort;
    private int initialTimeToLive;

    public Configuration() {
        nodeID = -1;
        address = null;
        networkHandlerType = Constants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = Constants.DEFAULT_ROUTING_STRATEGY;
        tcpListeningPort = Constants.DEFAULT_TCP_LISTENER_PORT;
        initialTimeToLive = Constants.DEFAULT_INITIAL_TIME_TO_LIVE;
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

    public int getInitialTimeToLive() {
        return initialTimeToLive;
    }

    public void setInitialTimeToLive(int initialTimeToLive) {
        this.initialTimeToLive = initialTimeToLive;
    }
}
