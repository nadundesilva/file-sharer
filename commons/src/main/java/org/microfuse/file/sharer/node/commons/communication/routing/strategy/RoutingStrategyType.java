package org.microfuse.file.sharer.node.commons.communication.routing.strategy;

/**
 * Routing strategy types.
 */
public enum RoutingStrategyType {
    UNSTRUCTURED_FLOODING("Unstructured Flooding"), UNSTRUCTURED_RANDOM_WALK("Super Peer Random Walk"),
    SUPER_PEER_FLOODING("Super-peer Flooding"), SUPER_PEER_RANDOM_WALK("Super-peer Random Walk");

    /**
     * Contains the value to be displayed.
     */
    private String value;

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
}
