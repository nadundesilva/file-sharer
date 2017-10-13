package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private String bootstrapServerIP;
    private int bootstrapServerPort;
    private String username;
    private String ip;
    private int peerListeningPort;
    private int listenerHandlingThreadCount;
    private int timeToLive;
    private int maxAssignedOrdinaryPeerCount;
    private int maxUnstructuredPeerCount;
    private int heartBeatInterval;
    private int gossipingInterval;
    private int networkHandlerTimeout;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;

    public Configuration() {
        bootstrapServerIP = NodeConstants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS;
        bootstrapServerPort = Constants.BOOTSTRAP_SERVER_PORT;
        username = NodeConstants.DEFAULT_USERNAME;
        ip = NodeConstants.DEFAULT_IP_ADDRESS;
        peerListeningPort = NodeConstants.DEFAULT_TCP_LISTENER_PORT;
        listenerHandlingThreadCount = NodeConstants.DEFAULT_LISTENER_HANDLER_THREAD_COUNT;
        timeToLive = NodeConstants.DEFAULT_TIME_TO_LIVE;
        maxAssignedOrdinaryPeerCount = NodeConstants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT;
        maxUnstructuredPeerCount = NodeConstants.DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT;
        heartBeatInterval = NodeConstants.DEFAULT_HEART_BEAT_INTERVAL;
        gossipingInterval = NodeConstants.DEFAULT_GOSSIPING_INTERVAL;
        networkHandlerTimeout = NodeConstants.DEFAULT_NETWORK_HANDLER_TIMEOUT;
        networkHandlerType = NodeConstants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = NodeConstants.DEFAULT_ROUTING_STRATEGY;
    }

    public Node getBootstrapServer() {
        Node node = new Node();
        node.setIp(bootstrapServerIP);
        node.setPort(bootstrapServerPort);
        node.setAlive(true);
        return node;
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

    public void setBootstrapServerPort(int bootstrapServerPort) {
        this.bootstrapServerPort = bootstrapServerPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public int getMaxUnstructuredPeerCount() {
        return maxUnstructuredPeerCount;
    }

    public void setMaxUnstructuredPeerCount(int maxUnstructuredPeerCount) {
        this.maxUnstructuredPeerCount = maxUnstructuredPeerCount;
    }

    public int getHeartBeatInterval() {
        return heartBeatInterval;
    }

    public void setHeartBeatInterval(int heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
    }

    public int getGossipingInterval() {
        return gossipingInterval;
    }

    public void setGossipingInterval(int gossipingInterval) {
        this.gossipingInterval = gossipingInterval;
    }

    public int getNetworkHandlerTimeout() {
        return networkHandlerTimeout;
    }

    public void setNetworkHandlerTimeout(int networkHandlerTimeout) {
        this.networkHandlerTimeout = networkHandlerTimeout;
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
}