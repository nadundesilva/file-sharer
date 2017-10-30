package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Routing strategy base abstract class.
 */
public abstract class RoutingStrategy {
    private static Map<RoutingStrategyType, Class<? extends RoutingStrategy>> routingStrategyClassMap;

    protected ServiceHolder serviceHolder;

    public RoutingStrategy(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;
    }

    static {
        // Populating the routing strategy class map
        routingStrategyClassMap = new HashMap<>();
        routingStrategyClassMap.put(RoutingStrategyType.UNSTRUCTURED_FLOODING,
                UnstructuredFloodingRoutingStrategy.class);
        routingStrategyClassMap.put(RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK,
                UnstructuredRandomWalkRoutingStrategy.class);
        routingStrategyClassMap.put(RoutingStrategyType.SUPER_PEER_FLOODING,
                SuperPeerFloodingRoutingStrategy.class);
        routingStrategyClassMap.put(RoutingStrategyType.SUPER_PEER_RANDOM_WALK,
                SuperPeerRandomWalkRoutingStrategy.class);
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

    /**
     * Get the name of the routing strategy.
     *
     * @return The name of the routing strategy
     */
    public abstract String getName();

    /**
     * Get the forwarding node based on the routing strategy.
     * The message is forwarded to all the nodes returned from this message.
     * The fromNode argument is null if this is the starting node of the message
     *
     * @param routingTable The routing table in the router
     * @param fromNode     The node from which this node received the message
     * @param message      The message to be routed
     * @return The forwarding nodes list
     */
    public abstract Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message);

    /**
     * Get a random node from a list of nodes.
     *
     * @param forwardingNodes The nodes from which the random node should be selected
     * @param fromNode        The node which sent the message to the current node
     * @return The random node selected from the list of nodes
     */
    protected Set<Node> getRandomNode(Set<Node> forwardingNodes, Node fromNode) {
        if (forwardingNodes != null) {
            if (fromNode != null) {
                forwardingNodes.remove(fromNode);
            }

            forwardingNodes = forwardingNodes.stream().parallel()
                    .filter(Node::isActive)
                    .collect(Collectors.toSet());

            int forwardNodeIndex = -1;
            if (forwardingNodes.size() > 1) {
                forwardNodeIndex = ThreadLocalRandom.current().nextInt(0, forwardingNodes.size() - 1);
            } else if (forwardingNodes.size() == 1) {
                forwardNodeIndex = 0;
            }

            if (forwardNodeIndex >= 0) {
                forwardingNodes = new HashSet<>(Collections.singletonList(
                        new ArrayList<>(forwardingNodes).get(forwardNodeIndex)
                ));
            }
        }

        if (forwardingNodes == null) {
            forwardingNodes = new HashSet<>();
        }
        if (forwardingNodes.size() == 0) {
            forwardingNodes.add(fromNode);
        }
        return forwardingNodes;
    }

    /**
     * Get the assigned super peer.
     *
     * @param ordinaryPeerRoutingTable The ordinary peer routing table
     * @return The set containing the assigned super peer
     */
    protected Set<Node> getAssignedSuperPeer(OrdinaryPeerRoutingTable ordinaryPeerRoutingTable) {
        Set<Node> forwardingNodes;
        Node superPeer = ordinaryPeerRoutingTable.getAssignedSuperPeer();
        if (superPeer != null && superPeer.isActive()) {
            forwardingNodes = new HashSet<>(Collections.singletonList(superPeer));
        } else {
            forwardingNodes = ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes();
            serviceHolder.getOverlayNetworkManager().searchForSuperPeer();
        }
        return forwardingNodes;
    }
}
