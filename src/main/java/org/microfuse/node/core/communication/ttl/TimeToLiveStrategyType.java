package org.microfuse.node.core.communication.ttl;

import java.util.HashMap;
import java.util.Map;

/**
 * Time to live determining strategy.
 */
public enum TimeToLiveStrategyType {
    FIXED("Fixed"), EXPANDING_RING("Expanding Ring");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    /**
     * The time to live strategy class map.
     * Maps the time to live strategy type to class.
     */
    private static Map<TimeToLiveStrategyType, Class<? extends TimeToLiveStrategy>> timeToLiveStrategyClassMap;

    static {
        // Populating the time to live strategy class map
        timeToLiveStrategyClassMap = new HashMap<>();
        timeToLiveStrategyClassMap.put(FIXED, FixedTimeToLiveStrategy.class);
    }

    TimeToLiveStrategyType(String value) {
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
     * Get the time to live strategy class based on time to live strategy type.
     *
     * @param timeToLiveStrategyType The time to live strategy type
     * @return The time to live strategy class
     */
    public static Class<? extends TimeToLiveStrategy> getRoutingStrategyClass(
            TimeToLiveStrategyType timeToLiveStrategyType) {
        return timeToLiveStrategyClassMap.get(timeToLiveStrategyType);
    }
}
