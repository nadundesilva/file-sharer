package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Peer types.
 */
public enum PeerType {
    SUPER_PEER("Super Peer"),
    ORDINARY_PEER("Ordinary Peer");

    /**
     * The routing table class map.
     * Maps the peer type to class.
     */
    private static Map<PeerType, Class<? extends RoutingTable>> routingTableClassMap;

    static {
        // Populating the routing table class map
        routingTableClassMap = new HashMap<>();
        routingTableClassMap.put(ORDINARY_PEER, OrdinaryPeerRoutingTable.class);
        routingTableClassMap.put(SUPER_PEER, SuperPeerRoutingTable.class);
    }

    /**
     * Contains the value to be displayed.
     */
    private String value;

    PeerType(String value) {
        this.value = value;
    }

    /**
     * Get the routing table class based on the peer type.
     *
     * @param networkHandlerType The peer type
     * @return The routing table class
     */
    public static Class<? extends RoutingTable> getRoutingTableClass(PeerType networkHandlerType) {
        return routingTableClassMap.get(networkHandlerType);
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
