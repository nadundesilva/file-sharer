package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The routing table containing the node information for super peers.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class SuperPeerRoutingTable extends RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRoutingTable.class);

    private Set<Node> superPeerNetworkNodes;
    private Set<Node> assignedOrdinaryPeerNodes;

    private final ReadWriteLock superPeerNetworkNodesLock;
    private final ReadWriteLock assignedOrdinaryPeerNodesLock;

    public SuperPeerRoutingTable(ServiceHolder serviceHolder) {
        super(serviceHolder);
        superPeerNetworkNodesLock = new ReentrantReadWriteLock();
        assignedOrdinaryPeerNodesLock = new ReentrantReadWriteLock();
        superPeerNetworkNodes = new HashSet<>();
        assignedOrdinaryPeerNodes = new HashSet<>();
    }

    public SuperPeerRoutingTable(ServiceHolder serviceHolder, OrdinaryPeerRoutingTable ordinaryPeerRoutingTable) {
        this(serviceHolder);
        ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes()
                .forEach(this::addUnstructuredNetworkRoutingTableEntry);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param ip   The ip of the node of the new entry
     * @param port The port of the node of the new entry
     * @return True if adding was successful
     */
    public boolean addSuperPeerNetworkRoutingTableEntry(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return addSuperPeerNetworkRoutingTableEntry(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param ip   The ip of the node of the new entry
     * @param port The port of the node of the new entry
     * @return True if removing was successful
     */
    public boolean removeSuperPeerNetworkRoutingTableEntry(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return removeSuperPeerNetworkRoutingTableEntry(node);
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllSuperPeerNetworkRoutingTableNodes() {
        return new HashSet<>(superPeerNetworkNodes);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getSuperPeerNetworkRoutingTableNode(String ip, int port) {
        Node requestedNode;
        superPeerNetworkNodesLock.readLock().lock();
        try {
            requestedNode = superPeerNetworkNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && node.getPort() == port)
                    .findAny()
                    .orElse(null);
        } finally {
            superPeerNetworkNodesLock.readLock().unlock();
        }
        return requestedNode;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param ip   The ip of the node of the new entry
     * @param port The port of the node of the new entry
     * @return True if adding was successful
     */
    public boolean addAssignedOrdinaryNetworkRoutingTableEntry(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return addAssignedOrdinaryNetworkRoutingTableEntry(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param ip   The ip of the node of the new entry
     * @param port The port of the node of the new entry
     * @return True if removing was successful
     */
    public boolean removeAssignedOrdinaryNetworkRoutingTableEntry(String ip, int port) {
        Node node = get(ip, port);
        if (node == null) {
            node = new Node(ip, port);
        }
        return removeAssignedOrdinaryNetworkRoutingTableEntry(node);
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllAssignedOrdinaryNetworkRoutingTableNodes() {
        return new HashSet<>(assignedOrdinaryPeerNodes);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getAssignedOrdinaryNetworkRoutingTableNode(String ip, int port) {
        Node requestedNode;
        assignedOrdinaryPeerNodesLock.readLock().lock();
        try {
            requestedNode = assignedOrdinaryPeerNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && Objects.equals(node.getPort(), port))
                    .findAny()
                    .orElse(null);
        } finally {
            assignedOrdinaryPeerNodesLock.readLock().unlock();
        }
        return requestedNode;
    }

    @Override
    public boolean removeFromAll(String ip, int port) {
        boolean isSuccessful = super.removeFromAll(ip, port);
        isSuccessful = removeSuperPeerNetworkRoutingTableEntry(ip, port) || isSuccessful;
        return removeAssignedOrdinaryNetworkRoutingTableEntry(ip, port) || isSuccessful;
    }

    @Override
    public Set<Node> getAll() {
        Set<Node> nodes = super.getAll();
        nodes.addAll(getAllSuperPeerNetworkRoutingTableNodes());
        nodes.addAll(getAllAssignedOrdinaryNetworkRoutingTableNodes());
        return nodes;
    }

    @Override
    public Node get(String ip, int port) {
        Node requestedNode = super.get(ip, port);
        if (requestedNode == null) {
            requestedNode = getAssignedOrdinaryNetworkRoutingTableNode(ip, port);
        }
        if (requestedNode == null) {
            requestedNode = getSuperPeerNetworkRoutingTableNode(ip, port);
        }
        return requestedNode;
    }

    @Override
    public void clear() {
        super.clear();
        superPeerNetworkNodesLock.writeLock().lock();
        try {
            superPeerNetworkNodes.clear();
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }
        assignedOrdinaryPeerNodesLock.writeLock().lock();
        try {
            assignedOrdinaryPeerNodes.clear();
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
    }

    @Override
    public void collectGarbage() {
        super.collectGarbage();
        superPeerNetworkNodesLock.writeLock().lock();
        try {
            removeInactiveNodesFromSet(superPeerNetworkNodes);
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }

        assignedOrdinaryPeerNodesLock.writeLock().lock();
        try {
            removeInactiveNodesFromSet(assignedOrdinaryPeerNodes);
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     * @return True if adding was successful
     */
    private boolean addSuperPeerNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        superPeerNetworkNodesLock.writeLock().lock();
        try {
            Node existingNode = get(node.getIp(), node.getPort());
            if (existingNode != null) {
                node = existingNode;
            }
            isSuccessful = superPeerNetworkNodes.add(node);
            if (isSuccessful) {
                logger.debug("Added node " + node.toString() + " to super peer network.");
            } else {
                logger.debug("Failed to add node " + node.toString() + " to super peer network.");
            }
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     * @return True if removing was successful
     */
    private boolean removeSuperPeerNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        superPeerNetworkNodesLock.writeLock().lock();
        try {
            isSuccessful = superPeerNetworkNodes.remove(node);
            if (isSuccessful) {
                logger.debug("Removed node " + node.toString() + " from super peer network.");
            } else {
                logger.debug("Failed to remove node " + node.toString() + " from super peer network.");
            }
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     * @return True if adding was successful
     */
    private boolean addAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        assignedOrdinaryPeerNodesLock.writeLock().lock();
        try {
            if (assignedOrdinaryPeerNodes.size() < serviceHolder.getConfiguration().getMaxAssignedOrdinaryPeerCount()) {
                Node existingNode = getUnstructuredNetworkRoutingTableNode(node.getIp(), node.getPort());
                if (existingNode != null) {
                    node = existingNode;
                }
                isSuccessful = assignedOrdinaryPeerNodes.add(node);
                if (isSuccessful) {
                    logger.debug("Added node " + node.toString() + " to assigned ordinary peers.");
                } else {
                    logger.debug("Failed to add node " + node.toString() + " to assigned ordinary peers.");
                }
            } else {
                isSuccessful = false;
                logger.debug("Assigned super peer node count already at maximum size "
                        + assignedOrdinaryPeerNodes.size());
            }
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     * @return True if removing was successful
     */
    private boolean removeAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        boolean isSuccessful;
        assignedOrdinaryPeerNodesLock.writeLock().lock();
        try {
            isSuccessful = assignedOrdinaryPeerNodes.remove(node);
            if (isSuccessful) {
                logger.debug("Remove node " + node.toString() + " from assigned ordinary peers.");
            } else {
                logger.debug("Failed to remove node " + node.toString() + " from assigned ordinary peers.");
            }
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }
}
