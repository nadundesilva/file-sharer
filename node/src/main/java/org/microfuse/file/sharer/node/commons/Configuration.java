package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.utils.Constants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private String username;
    private String bootstrapServerIP;
    private int bootstrapServerPort;
    private String ip;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;
    private int peerListeningPort;
    private int listenerHandlingThreadCount;
    private int timeToLive;
    private int maxAssignedOrdinaryPeerCount;

    public Configuration() {
        username = Constants.DEFAULT_USERNAME;
        bootstrapServerIP = Constants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS;
        bootstrapServerPort = Constants.DEFAULT_BOOTSTRAP_SERVER_PORT;
        ip = Constants.DEFAULT_IP_ADDRESS;
        networkHandlerType = Constants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = Constants.DEFAULT_ROUTING_STRATEGY;
        peerListeningPort = Constants.DEFAULT_TCP_LISTENER_PORT;
        listenerHandlingThreadCount = Constants.DEFAULT_LISTENER_HANDLER_THREAD_COUNT;
        timeToLive = Constants.DEFAULT_TIME_TO_LIVE;
        maxAssignedOrdinaryPeerCount = Constants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBootstrapServerIP() {
        return bootstrapServerIP;
    }

    public void setBootstrapServerIP(String bootstrapServerIP) {
        this.bootstrapServerIP = bootstrapServerIP;
    }

    public int getBootstrapServerPort() {
        return bootstrapServerPort;
    }

    public Node getBootstrapServer() {
        Node node = new Node();
        node.setIp(bootstrapServerIP);
        node.setPort(bootstrapServerPort);
        node.setAlive(true);
        return node;
    }

    public void setBootstrapServerPort(int bootstrapServerPort) {
        this.bootstrapServerPort = bootstrapServerPort;
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

    public int getListenerHandlingThreadCount() {
        return listenerHandlingThreadCount;
    }

    public void setListenerHandlingThreadCount(int listenerHandlingThreadCount) {
        this.listenerHandlingThreadCount = listenerHandlingThreadCount;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public int getMaxAssignedOrdinaryPeerCount() {
        return maxAssignedOrdinaryPeerCount;
    }

    public void setMaxAssignedOrdinaryPeerCount(int maxAssignedOrdinaryPeerCount) {
        this.maxAssignedOrdinaryPeerCount = maxAssignedOrdinaryPeerCount;
    }
}
