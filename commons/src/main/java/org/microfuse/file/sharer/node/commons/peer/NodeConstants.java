package org.microfuse.file.sharer.node.commons.peer;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;

/**
 * Constants used by the Node.
 */
public class NodeConstants {
    public static final String DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_USERNAME_PREFIX = "microfuse.2017_";
    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_TRACER_SERVE_IP = "127.0.0.1";
    public static final String DEFAULT_RMI_REGISTRY_ENTRY_PREFIX = "microfuse.2017.RMI_";
    public static final int DEFAULT_TRACER_SERVE_PORT = 9999;
    public static final int DEFAULT_PEER_LISTENING_PORT = 4444;
    public static final int DEFAULT_NETWORK_HANDLER_THREAD_COUNT = 5;
    public static final int DEFAULT_TIME_TO_LIVE = 20;
    public static final int DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT = 2;
    public static final int DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT = 4;
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 30000;
    public static final int DEFAULT_GOSSIPING_INTERVAL = 60000;
    public static final int DEFAULT_NETWORK_HANDLER_SEND_TIMEOUT = 3000;
    public static final int DEFAULT_NETWORK_HANDLER_REPLY_TIMEOUT = 6000;
    public static final int DEFAULT_SER_SUPER_PEER_TIMEOUT = 5000;
    public static final int DEFAULT_AUTOMATED_GARBAGE_COLLECTION_INTERVAL = 300000;
    public static final int DEFAULT_UDP_NETWORK_HANDLER_RETRY_INTERVAL = 1000;
    public static final int DEFAULT_UDP_NETWORK_HANDLER_RETRY_COUNT = 3;
    public static final NetworkHandlerType DEFAULT_NETWORK_HANDLER = NetworkHandlerType.TCP_SOCKET;
    public static final RoutingStrategyType DEFAULT_ROUTING_STRATEGY = RoutingStrategyType.SUPER_PEER_FLOODING;

    public static final String CONFIG_FILE = "config.json";
    public static final int INITIAL_HOP_COUNT = 0;

    private NodeConstants() {   // Preventing from being initiated
    }
}
