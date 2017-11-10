package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The routing table containing the node information.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public abstract class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    protected ServiceHolder serviceHolder;

    private static Map<PeerType, Class<? extends RoutingTable>> routingTableClassMap;

    private Set<Node> unstructuredNetworkNodes;

    private final ReadWriteLock unstructuredNetworkNodesLock;

    static {
        // Populating the routing table class map
        routingTableClassMap = new HashMap<>();
        routingTableClassMap.put(PeerType.ORDINARY_PEER, OrdinaryPeerRoutingTable.class);
        routingTableClassMap.put(PeerType.SUPER_PEER, SuperPeerRoutingTable.class);
    }

    public RoutingTable(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;
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
     * @param ip   The ip of the node of the new entry
     * @param port The port of the node of the new entry
     * @return True if adding was successful
     */
    public boolean addUnstructuredNetworkRoutingTableEntry(String ip, int port) {
        if (!(Objects.equals(ip, serviceHolder.getConfiguration().getIp())
                && port == serviceHolder.getConfiguration().getPeerListeningPort())) {
            Node node = get(ip, port);
            if (node == null) {
                node = new Node(ip, port);
            }
            return addUnstructuredNetworkRoutingTableEntry(node);
        } else {
            logger.info("Dropped request to add self");
            return false;
        }
    }

    /**
     * Remove a node form the unstructured network routing table entry.
     *
     * @param ip   The ip of the node to be removed
     * @param port The port of the node to be removed
     * @return True if adding was successful
     */
    public boolean removeUnstructuredNetworkRoutingTableEntry(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return removeUnstructuredNetworkRoutingTableEntry(node);
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllUnstructuredNetworkNodes() {
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
     * @param ip   The ip of the node to be removed
     * @param port The port of the node to be removed
     * @return True if removing was successful
     */
    public boolean removeFromAll(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return removeUnstructuredNetworkRoutingTableEntry(node);
    }

    /**
     * Get all the nodes registered in the routing table.
     *
     * @return The nodes registered
     */
    public Set<Node> getAll() {
        return getAllUnstructuredNetworkNodes();
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
            getAllUnstructuredNetworkNodes().forEach(this::removeUnstructuredNetworkRoutingTableEntry);
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
    }

    /**
     * Collect the garbage nodes in the routing table.
     */
    public void collectGarbage() {
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            removeInactiveNodesFromSet(unstructuredNetworkNodes);
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     * @return True if adding was successful
     */
    protected boolean addUnstructuredNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetworkNodes.add(node);
            if (isSuccessful) {
                logger.info("Added node " + node.toString() + " to unstructured network.");
            } else {
                logger.info("Failed to add node " + node.toString() + " to unstructured network.");
            }
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Remove a node form the unstructured network routing table entry.
     *
     * @param node The node of the new entry
     * @return True if removing was successful
     */
    protected boolean removeUnstructuredNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        unstructuredNetworkNodesLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetworkNodes.remove(node);
            if (isSuccessful) {
                logger.info("Removed node " + node.toString() + " from unstructured network.");
            } else {
                logger.info("Failed to remove node " + node.toString() + " from unstructured network.");
            }
        } finally {
            unstructuredNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    protected void removeInactiveNodesFromSet(Set<Node> nodeSet) {
        Set<Node> garbageNodes = nodeSet.stream().parallel()
                .filter(node -> !node.isActive())
                .collect(Collectors.toSet());

        garbageNodes.forEach(node -> {
            logger.info("Removed inactive node " + node.toString() + " from the routing table");
            removeFromAll(node.getIp(), node.getPort());
        });
    }
}
