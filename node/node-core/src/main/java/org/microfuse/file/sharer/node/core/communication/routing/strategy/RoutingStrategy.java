package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(RoutingStrategy.class);

    private static Map<RoutingStrategyType, Class<? extends RoutingStrategy>> routingStrategyClassMap;

    protected ServiceHolder serviceHolder;

    private Map<Message, CacheEntry> serMessageCache;
    private Map<Message, CacheEntry> serSuperPeerMessageCache;

    public RoutingStrategy(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;

        serMessageCache = new HashMap<>();
        serSuperPeerMessageCache = new HashMap<>();
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
     * Collect the garbage cache in routing strategy.
     */
    public void collectGarbage() {
        long currentTimeStamp = System.currentTimeMillis();

        // Removing old entries from the SER message cache
        Set<Message> serEntriesToBeRemoved = serMessageCache.entrySet().stream().parallel()
                .filter(messageCacheEntryEntry ->
                        messageCacheEntryEntry.getValue().getTimeStamp()
                                + serviceHolder.getConfiguration().getAutomatedGarbageCollectionInterval()
                                < currentTimeStamp)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        serEntriesToBeRemoved.forEach(message -> serMessageCache.remove(message));

        // Removing old entries from the SER_SUPER_PEER message cache
        Set<Message> serSuperPeerEntriesToBeRemoved = serSuperPeerMessageCache.entrySet().stream().parallel()
                .filter(messageCacheEntryEntry ->
                        messageCacheEntryEntry.getValue().getTimeStamp()
                                + serviceHolder.getConfiguration().getAutomatedGarbageCollectionInterval()
                                < currentTimeStamp)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        serSuperPeerEntriesToBeRemoved.forEach(message -> serSuperPeerMessageCache.remove(message));
    }

    /**
     * Get a random node from a list of nodes.
     *
     * @param message         The message which will be sent
     * @param forwardingNodes The nodes from which the random node should be selected
     * @param fromNode        The node which sent the message to the current node
     * @return The random node selected from the list of nodes
     */
    protected Set<Node> getRandomNode(Message message, Set<Node> forwardingNodes, Node fromNode) {
        if (forwardingNodes != null) {
            // Remove the node which sent the
            if (fromNode != null) {
                forwardingNodes.remove(fromNode);
            }

            // Removing inactive nodes
            forwardingNodes = forwardingNodes.stream().parallel()
                    .filter(Node::isActive)
                    .collect(Collectors.toSet());

            // Removing nodes to which this message had been already routed to
            forwardingNodes = filterUnCachedNodes(message, forwardingNodes);

            // Selecting a random node
            int forwardNodeIndex = -1;
            if (forwardingNodes.size() > 0) {
                forwardNodeIndex = ThreadLocalRandom.current().nextInt(0, forwardingNodes.size());
            }
            if (forwardNodeIndex >= 0) {
                forwardingNodes = new HashSet<>(Collections.singletonList(
                        new ArrayList<>(forwardingNodes).get(forwardNodeIndex)
                ));
            }
        }

        // Creating empty set no nodes are available
        if (forwardingNodes == null) {
            forwardingNodes = new HashSet<>();
        }

        // Sending back to the same node if no nodes are present
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
            forwardingNodes = ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes();
            serviceHolder.getOverlayNetworkManager().searchForSuperPeer();
        }
        return forwardingNodes;
    }

    /**
     * Gets the list of uncached nodes from a list of nodes.
     * The current forwarding nodes are added to the cache as well.
     *
     * @param message         The message to be cached
     * @param forwardingNodes The nodes to be filtered
     * @return The nodes which are not currently in the cache
     */
    protected Set<Node> filterUnCachedNodes(Message message, Set<Node> forwardingNodes) {
        CacheEntry cacheEntry = null;
        MessageType messageType = message.getType();
        if (messageType == MessageType.SER) {
            cacheEntry = serMessageCache.computeIfAbsent(message, k -> new CacheEntry());
        } else if (messageType == MessageType.SER_SUPER_PEER) {
            cacheEntry = serSuperPeerMessageCache.computeIfAbsent(message, k -> new CacheEntry());
        } else {
            logger.debug("Unknown type of message"
                    + (messageType == null ? "" : " belonging to type " + messageType.getValue())
                    + " not added to the cache");
        }

        if (cacheEntry != null) {
            cacheEntry.setTimeStamp(System.currentTimeMillis());

            collectGarbage();
            forwardingNodes.removeAll(cacheEntry.getAllForwardedNodes());

            cacheEntry.addAllForwardedNodes(forwardingNodes);
        }

        return forwardingNodes;
    }

    /**
     * Notify the routing strategy of failed sent messages.
     *
     * @param message The message which was sent
     * @param node    The node to which the message was sent
     */
    public void notifyFailedMessages(Message message, Node node) {
        MessageType messageType = message.getType();
        if (messageType == MessageType.SER) {
            serMessageCache.get(message).removeForwardedNode(node);
        } else if (messageType == MessageType.SER_SUPER_PEER) {
            serSuperPeerMessageCache.get(message).removeForwardedNode(node);
        } else {
            logger.debug("Unknown type of message"
                    + (messageType == null ? "" : " belonging to type " + messageType.getValue())
                    + " not added to the cache");
        }
    }
}
