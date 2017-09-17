package org.microfuse.node.core.utils;

import org.microfuse.node.core.communication.network.NetworkHandlerType;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.node.core.communication.ttl.TimeToLiveStrategyType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants used by the Node.
 */
public class Constants {
    private Constants() {   // Preventing from being initiated
    }

    public static final NetworkHandlerType DEFAULT_NETWORK_HANDLER = NetworkHandlerType.SOCKET;
    public static final RoutingStrategyType DEFAULT_ROUTING_STRATEGY = RoutingStrategyType.FLOODING;
    public static final TimeToLiveStrategyType DEFAULT_TIME_TO_LIVE_STRATEGY = TimeToLiveStrategyType.FIXED;

    public static final int DEFAULT_TCP_LISTENER_PORT = 4444;
    public static final int DEFAULT_INITIAL_TIME_TO_LIVE = 5;
    public static final int UNASSIGNED_TIME_TO_LIVE = -Integer.MAX_VALUE;

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
}
