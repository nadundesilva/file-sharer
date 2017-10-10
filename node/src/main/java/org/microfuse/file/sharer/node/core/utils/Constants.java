package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants used by the Node.
 */
public class Constants {
    public static final NetworkHandlerType DEFAULT_NETWORK_HANDLER = NetworkHandlerType.TCP_SOCKET;
    public static final RoutingStrategyType DEFAULT_ROUTING_STRATEGY = RoutingStrategyType.UNSTRUCTURED_FLOODING;
    public static final PeerType DEFAULT_PEER_TYPE = PeerType.ORDINARY_PEER;

    public static final String DEFAULT_USERNAME = "microfuse.2017";
    public static final String DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_BOOTSTRAP_SERVER_PORT = 5555;
    public static final String DEFAULT_IP_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_TCP_LISTENER_PORT = 4444;
    public static final int DEFAULT_LISTENER_HANDLER_THREAD_COUNT = 5;
    public static final int DEFAULT_TIME_TO_LIVE = 5;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static final String CONFIG_FILE = "config.json";
    public static final int INITIAL_HOP_COUNT = 0;
    public static final int BOOTSTRAP_SERVER_LISTENER_PORT = 55555;

    private Constants() {   // Preventing from being initiated
    }
}
