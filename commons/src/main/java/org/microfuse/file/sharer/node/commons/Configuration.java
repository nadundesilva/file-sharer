package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.NodeState;

/**
 * Configuration of this Node.
 */
public class Configuration {
    private String bootstrapServerIP;
    private int bootstrapServerPort;
    private String usernamePrefix;
    private String ip;
    private int peerListeningPort;
    private int networkHandlerThreadCount;
    private String rmiRegistryEntryPrefix;
    private int timeToLive;
    private int maxAssignedOrdinaryPeerCount;
    private int maxUnstructuredPeerCount;
    private int maxSuperPeerCount;
    private int heartbeatInterval;
    private int gossipingInterval;
    private int bootstrapServerReplyWaitTimeout;
    private int serSuperPeerTimeout;
    private int automatedGarbageCollectionInterval;
    private int udpNetworkHandlerRetryInterval;
    private int udpNetworkHandlerRetryCount;
    private String tracerIP;
    private int tracerPort;
    private NetworkHandlerType networkHandlerType;
    private RoutingStrategyType routingStrategyType;

    public Configuration() {
        loadDefaults();
    }

    public void loadDefaults() {
        bootstrapServerIP = NodeConstants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS;
        bootstrapServerPort = Constants.BOOTSTRAP_SERVER_PORT;
        usernamePrefix = NodeConstants.DEFAULT_USERNAME_PREFIX;
        ip = NodeConstants.DEFAULT_IP_ADDRESS;
        peerListeningPort = NodeConstants.DEFAULT_PEER_LISTENING_PORT;
        networkHandlerThreadCount = NodeConstants.DEFAULT_NETWORK_HANDLER_THREAD_COUNT;
        rmiRegistryEntryPrefix = NodeConstants.DEFAULT_RMI_REGISTRY_ENTRY_PREFIX;
        timeToLive = NodeConstants.DEFAULT_TIME_TO_LIVE;
        maxAssignedOrdinaryPeerCount = NodeConstants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT;
        maxUnstructuredPeerCount = NodeConstants.DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT;
        maxSuperPeerCount = NodeConstants.DEFAULT_MAX_SUPER_PEER_COUNT;
        heartbeatInterval = NodeConstants.DEFAULT_HEARTBEAT_INTERVAL;
        gossipingInterval = NodeConstants.DEFAULT_GOSSIPING_INTERVAL;
        bootstrapServerReplyWaitTimeout = NodeConstants.DEFAULT_NETWORK_HANDLER_REPLY_TIMEOUT;
        serSuperPeerTimeout = NodeConstants.DEFAULT_SER_SUPER_PEER_TIMEOUT;
        automatedGarbageCollectionInterval = NodeConstants.DEFAULT_AUTOMATED_GARBAGE_COLLECTION_INTERVAL;
        udpNetworkHandlerRetryInterval = NodeConstants.DEFAULT_UDP_NETWORK_HANDLER_RETRY_INTERVAL;
        udpNetworkHandlerRetryCount = NodeConstants.DEFAULT_UDP_NETWORK_HANDLER_RETRY_COUNT;
        tracerIP = NodeConstants.DEFAULT_TRACER_IP;
        tracerPort = NodeConstants.DEFAULT_TRACER_PORT;
        networkHandlerType = NodeConstants.DEFAULT_NETWORK_HANDLER;
        routingStrategyType = NodeConstants.DEFAULT_ROUTING_STRATEGY;
    }

    public Node getBootstrapServer() {
        Node node = new Node();
        node.setIp(bootstrapServerIP);
        node.setPort(bootstrapServerPort);
        node.setState(NodeState.ACTIVE);
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

    public String getUsernamePrefix() {
        return usernamePrefix;
    }

    public void setUsernamePrefix(String usernamePrefix) {
        this.usernamePrefix = usernamePrefix;
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

    public int getNetworkHandlerThreadCount() {
        return networkHandlerThreadCount;
    }

    public void setNetworkHandlerThreadCount(int networkHandlerThreadCount) {
        this.networkHandlerThreadCount = networkHandlerThreadCount;
    }

    public String getRmiRegistryEntryPrefix() {
        return rmiRegistryEntryPrefix;
    }

    public void setRmiRegistryEntryPrefix(String rmiRegistryEntryPrefix) {
        this.rmiRegistryEntryPrefix = rmiRegistryEntryPrefix;
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

    public int getMaxSuperPeerCount() {
        return maxSuperPeerCount;
    }

    public void setMaxSuperPeerCount(int maxSuperPeerCount) {
        this.maxSuperPeerCount = maxSuperPeerCount;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getGossipingInterval() {
        return gossipingInterval;
    }

    public void setGossipingInterval(int gossipingInterval) {
        this.gossipingInterval = gossipingInterval;
    }

    public int getBootstrapServerReplyWaitTimeout() {
        return bootstrapServerReplyWaitTimeout;
    }

    public void setBootstrapServerReplyWaitTimeout(int bootstrapServerReplyWaitTimeout) {
        this.bootstrapServerReplyWaitTimeout = bootstrapServerReplyWaitTimeout;
    }

    public int getSerSuperPeerTimeout() {
        return serSuperPeerTimeout;
    }

    public void setSerSuperPeerTimeout(int serSuperPeerTimeout) {
        this.serSuperPeerTimeout = serSuperPeerTimeout;
    }

    public int getAutomatedGarbageCollectionInterval() {
        return automatedGarbageCollectionInterval;
    }

    public void setAutomatedGarbageCollectionInterval(int automatedGarbageCollectionInterval) {
        this.automatedGarbageCollectionInterval = automatedGarbageCollectionInterval;
    }

    public int getUdpNetworkHandlerRetryInterval() {
        return udpNetworkHandlerRetryInterval;
    }

    public void setUdpNetworkHandlerRetryInterval(int udpNetworkHandlerRetryInterval) {
        this.udpNetworkHandlerRetryInterval = udpNetworkHandlerRetryInterval;
    }

    public int getUdpNetworkHandlerRetryCount() {
        return udpNetworkHandlerRetryCount;
    }

    public void setUdpNetworkHandlerRetryCount(int udpNetworkHandlerRetryCount) {
        this.udpNetworkHandlerRetryCount = udpNetworkHandlerRetryCount;
    }

    public String getTracerIP() {
        return tracerIP;
    }

    public void setTracerIP(String tracerIP) {
        this.tracerIP = tracerIP;
    }

    public int getTracerPort() {
        return tracerPort;
    }

    public void setTracerPort(int tracerPort) {
        this.tracerPort = tracerPort;
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
