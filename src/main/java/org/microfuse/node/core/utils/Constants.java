package org.microfuse.node.core.utils;

import org.microfuse.node.core.communication.network.NetworkHandlerType;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategyType;

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
    public static final int DEFAULT_STARTING_TIME_TO_LIVE = 20;

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
}
