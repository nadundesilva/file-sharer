package org.microfuse.node.core.communication.routing.strategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Routing strategy types.
 */
public enum RoutingStrategyType {
    FLOODING("Flooding"), RANDOM_WALK("Random Walk");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    /**
     * The routing strategy class map.
     * Maps the routing strategy type to class
     */
    private static Map<RoutingStrategyType, Class<? extends RoutingStrategy>> routingStrategyClassMap;

    static {
        // Populating the routing strategy class map
        routingStrategyClassMap = new HashMap<>();
        routingStrategyClassMap.put(RoutingStrategyType.FLOODING, FloodingRoutingStrategy.class);
        routingStrategyClassMap.put(RoutingStrategyType.RANDOM_WALK, RandomWalkRoutingStrategy.class);
    }

    RoutingStrategyType(String value) {
        this.value = value;
    }

    /**
     * Get the value to be displayed.
     *
     * @return The value to be displayed
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the routing strategy class based on routing strategy type.
     *
     * @param routingStrategyType The routing strategy type
     * @return The routing strategy class
     */
    public static Class<? extends RoutingStrategy> getRoutingStrategyClass(RoutingStrategyType routingStrategyType) {
        return routingStrategyClassMap.get(routingStrategyType);
    }
}
