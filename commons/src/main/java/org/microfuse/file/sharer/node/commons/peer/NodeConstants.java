package org.microfuse.file.sharer.node.commons.peer;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;

/**
 * Constants used by the Node.
 */
public class NodeConstants {
    public static final String DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_USERNAME = "microfuse.2017";
    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_SEND_PORT = 9999;
    public static final int DEFAULT_TCP_LISTENER_PORT = 4444;
    public static final int DEFAULT_LISTENER_HANDLER_THREAD_COUNT = 5;
    public static final int DEFAULT_TIME_TO_LIVE = 5;
    public static final int DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT = 5;
    public static final int DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT = 3;
    public static final int DEFAULT_HEART_BEAT_INTERVAL = 60;
    public static final int DEFAULT_GOSSIPING_INTERVAL = 120;
    public static final int DEFAULT_NETWORK_HANDLER_TIMEOUT = 3;
    public static final NetworkHandlerType DEFAULT_NETWORK_HANDLER = NetworkHandlerType.TCP_SOCKET;
    public static final RoutingStrategyType DEFAULT_ROUTING_STRATEGY = RoutingStrategyType.UNSTRUCTURED_FLOODING;

    public static final String CONFIG_FILE = "config.json";
    public static final int INITIAL_HOP_COUNT = 0;

    private NodeConstants() {   // Preventing from being initiated
    }
}
