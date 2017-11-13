package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
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
    private static final long serialVersionUID = 105L;
    private static final transient Logger logger = LoggerFactory.getLogger(SuperPeerRoutingTable.class);

    private Set<Node> superPeerNetworkNodes;
    private Set<Node> assignedOrdinaryPeerNodes;

    private ReadWriteLock superPeerNetworkNodesLock;
    private ReadWriteLock assignedOrdinaryPeerNodesLock;

    public SuperPeerRoutingTable(ServiceHolder serviceHolder) {
        super(serviceHolder);
        superPeerNetworkNodesLock = new ReentrantReadWriteLock();
        assignedOrdinaryPeerNodesLock = new ReentrantReadWriteLock();
        superPeerNetworkNodes = new HashSet<>();
        assignedOrdinaryPeerNodes = new HashSet<>();
    }

    public SuperPeerRoutingTable(ServiceHolder serviceHolder, OrdinaryPeerRoutingTable ordinaryPeerRoutingTable) {
        this(serviceHolder);

        // Copying all the unstructured nodes
        ordinaryPeerRoutingTable.getAllUnstructuredNetworkNodes()
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
        if (!(Objects.equals(ip, serviceHolder.getConfiguration().getIp())
                && port == serviceHolder.getConfiguration().getPeerListeningPort())) {
            Node node = get(ip, port);
            if (node == null) {
                node = new Node(ip, port);
            }
            return addSuperPeerNetworkRoutingTableEntry(node);
        } else {
            logger.info("Dropped request to add self");
            return false;
        }
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
    public Set<Node> getAllSuperPeerNetworkNodes() {
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
        if (!(Objects.equals(ip, serviceHolder.getConfiguration().getIp())
                && port == serviceHolder.getConfiguration().getPeerListeningPort())) {
            Node node = get(ip, port);
            if (node == null) {
                node = new Node(ip, port);
            }
            return addAssignedOrdinaryNetworkRoutingTableEntry(node);
        } else {
            logger.info("Dropped request to add self");
            return false;
        }
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
    public Set<Node> getAllAssignedOrdinaryNetworkNodes() {
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
        nodes.addAll(getAllSuperPeerNetworkNodes());
        nodes.addAll(getAllAssignedOrdinaryNetworkNodes());
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
            getAllSuperPeerNetworkNodes().forEach(this::removeSuperPeerNetworkRoutingTableEntry);
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }
        assignedOrdinaryPeerNodesLock.writeLock().lock();
        try {
            getAllAssignedOrdinaryNetworkNodes()
                    .forEach(this::removeAssignedOrdinaryNetworkRoutingTableEntry);
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
    }

    @Override
    public void collectGarbage() {
        super.collectGarbage();

        // Removing inactive super peer network nodes
        superPeerNetworkNodesLock.writeLock().lock();
        try {
            removeInactiveNodesFromSet(superPeerNetworkNodes);
        } finally {
            superPeerNetworkNodesLock.writeLock().unlock();
        }

        // Removing inactive assigned ordinary peer nodes
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
            if (superPeerNetworkNodes.size() < serviceHolder.getConfiguration().getMaxSuperPeerCount()) {
                Node existingNode = get(node.getIp(), node.getPort());
                if (existingNode != null) {
                    node = existingNode;
                }
                isSuccessful = superPeerNetworkNodes.add(node);
                if (isSuccessful) {
                    logger.info("Added node " + node.toString() + " to super peer network.");

                    // Notifying the tracer
                    Tracer tracer = serviceHolder.getTracer();
                    if (tracer != null) {
                        try {
                            tracer.addSuperPeerNetworkConnection(
                                    serviceHolder.getConfiguration().getIp(),
                                    serviceHolder.getConfiguration().getPeerListeningPort(),
                                    node.getIp(), node.getPort()
                            );
                        } catch (RemoteException e) {
                            logger.warn("Failed to add super peer network connection to the tracer", e);
                        }
                    }
                } else {
                    logger.info("Failed to add node " + node.toString() + " to super peer network.");
                }
            } else {
                isSuccessful = false;
                logger.info("Super peer network node count already at maximum size "
                        + serviceHolder.getConfiguration().getMaxSuperPeerCount());
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
                logger.info("Removed node " + node.toString() + " from super peer network.");

                // Notifying the tracer
                Tracer tracer = serviceHolder.getTracer();
                if (tracer != null) {
                    try {
                        tracer.removeSuperPeerNetworkConnection(
                                serviceHolder.getConfiguration().getIp(),
                                serviceHolder.getConfiguration().getPeerListeningPort(),
                                node.getIp(), node.getPort()
                        );
                    } catch (RemoteException e) {
                        logger.warn("Failed to remove super peer network connection from the tracer", e);
                    }
                }
            } else {
                logger.info("Failed to remove node " + node.toString() + " from super peer network.");
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
                    logger.info("Added node " + node.toString() + " to assigned ordinary peers.");

                    // Notifying the tracer
                    Tracer tracer = serviceHolder.getTracer();
                    if (tracer != null) {
                        try {
                            tracer.addAssignedOrdinaryPeerConnection(
                                    serviceHolder.getConfiguration().getIp(),
                                    serviceHolder.getConfiguration().getPeerListeningPort(),
                                    node.getIp(), node.getPort()
                            );
                        } catch (RemoteException e) {
                            logger.warn("Failed to add assigned ordinary peer network connection to the tracer", e);
                        }
                    }
                } else {
                    logger.info("Failed to add node " + node.toString() + " to assigned ordinary peers.");
                }
            } else {
                isSuccessful = false;
                logger.info("Assigned super peer node count already at maximum size "
                        + serviceHolder.getConfiguration().getMaxAssignedOrdinaryPeerCount());
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
                logger.info("Remove node " + node.toString() + " from assigned ordinary peers.");

                // Notifying the tracer
                Tracer tracer = serviceHolder.getTracer();
                if (tracer != null) {
                    try {
                        tracer.removeAssignedOrdinaryPeerConnection(
                                serviceHolder.getConfiguration().getIp(),
                                serviceHolder.getConfiguration().getPeerListeningPort(),
                                node.getIp(), node.getPort()
                        );
                    } catch (RemoteException e) {
                        logger.warn("Failed to remove assigned ordinary peer network connection from the tracer", e);
                    }
                }
            } else {
                logger.info("Failed to remove node " + node.toString() + " from assigned ordinary peers.");
            }
        } finally {
            assignedOrdinaryPeerNodesLock.writeLock().unlock();
        }
        return isSuccessful;
    }
}
