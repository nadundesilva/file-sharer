package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants used by the Node.
 */
public class Constants {
    public static final NetworkHandlerType DEFAULT_NETWORK_HANDLER = NetworkHandlerType.SOCKET;
    public static final RoutingStrategyType DEFAULT_ROUTING_STRATEGY = RoutingStrategyType.FLOODING;
    public static final int DEFAULT_TCP_LISTENER_PORT = 4444;
    public static final int DEFAULT_INITIAL_TIME_TO_LIVE = 5;
    public static final int UNASSIGNED_TIME_TO_LIVE = -Integer.MAX_VALUE;
    public static final int BOOTSTRAP_SERVER_LISTENER_PORT = 55555;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Constants() {   // Preventing from being initiated
    }
}
