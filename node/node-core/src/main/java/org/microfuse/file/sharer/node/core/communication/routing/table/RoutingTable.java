package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The routing table containing the node information.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public abstract class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private static Map<PeerType, Class<? extends RoutingTable>> routingTableClassMap;

    private Set<Node> unstructuredNetworkNodes;

    private final ReadWriteLock unstructuredNetworkNodesLock;

    static {
        // Populating the routing table class map
        routingTableClassMap = new HashMap<>();
        routingTableClassMap.put(PeerType.ORDINARY_PEER, OrdinaryPeerRoutingTable.class);
        routingTableClassMap.put(PeerType.SUPER_PEER, SuperPeerRoutingTable.class);
    }

    public RoutingTable() {
        unstructuredNetworkNodesLock = new ReentrantReadWriteLock();
        unstructuredNetworkNodes = new HashSet<>();
    }

    /**
     * Get the routing table class based on the peer type.
     *
     * @param peerType The peer type
     * @return The routing table class
     */
    public static Class<? extends RoutingTable> getRoutingTableClass(PeerType peerType) {
        return routingTableClassMap.get(peerType);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public boolean addUnstructuredNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetworkNodes.add(node);
            if (isSuccessful) {
                logger.debug("Added node " + node.toString() + " to unstructured network.");
            } else {
                logger.debug("Failed to add node " + node.toString() + " to unstructured network.");
            }
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Add all nodes into the unstructured network from a collection.
     *
     * @param nodes The nodes to be added
     */
    public void addAllUnstructuredNetworkRoutingTableEntry(Collection<Node> nodes) {
        unstructuredNetworkNodesLock.writeLock().lock();
        nodes.forEach(this::addUnstructuredNetworkRoutingTableEntry);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public boolean removeUnstructuredNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetworkNodes.remove(node);
            if (isSuccessful) {
                logger.debug("Removed node " + node.toString() + " from unstructured network.");
            } else {
                logger.debug("Failed to remove node " + node.toString() + " from unstructured network.");
            }
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllUnstructuredNetworkRoutingTableNodes() {
        return new HashSet<>(unstructuredNetworkNodes);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getUnstructuredNetworkRoutingTableNode(String ip, int port) {
        Node requestedNode;
        unstructuredNetworkNodesLock.readLock().lock();
        try {
            requestedNode = unstructuredNetworkNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && node.getPort() == port)
                    .findAny()
                    .orElse(null);
        } finally {
            unstructuredNetworkNodesLock.readLock().unlock();
        }
        return requestedNode;
    }

    /**
     * Remove a node from all tables.
     *
     * @param node The node to be removed
     */
    public boolean removeFromAll(Node node) {
        return removeUnstructuredNetworkRoutingTableEntry(node);
    }

    /**
     * Get all the nodes registered in the routing table.
     *
     * @return The nodes registered
     */
    public Set<Node> getAll() {
        return getAllUnstructuredNetworkRoutingTableNodes();
    }

    /**
     * Get a node searching all the nodes.
     *
     * @param ip   The ip of the node to fetch
     * @param port The port of the node to fetch
     * @return The node requested
     */
    public Node get(String ip, int port) {
        return getUnstructuredNetworkRoutingTableNode(ip, port);
    }

    /**
     * Clear the routing table.
     */
    public void clear() {
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            unstructuredNetworkNodes.clear();
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
    }
}