package org.microfuse.file.sharer.node.core.tracing;

import com.google.gson.annotations.Expose;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.commons.tracing.NetworkConnection;
import org.microfuse.file.sharer.node.commons.tracing.TraceableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * The network class containing the structure of the network formed by the file sharers.
 */
public class Network {
    private static final Logger logger = LoggerFactory.getLogger(Network.class);

    @Expose
    private Set<NetworkConnection> unstructuredNetwork;
    @Expose
    private Set<NetworkConnection> superPeerNetwork;
    @Expose
    private Set<NetworkConnection> assignedSuperPeersNetwork;
    @Expose
    private Set<TraceableNode> nodes;

    @Expose(serialize = false)
    private ReadWriteLock unstructuredNetworkLock;
    @Expose(serialize = false)
    private ReadWriteLock superPeerNetworkLock;
    @Expose(serialize = false)
    private ReadWriteLock assignedSuperPeersNetworkLock;
    @Expose(serialize = false)
    private Lock nodesLock;

    public Network() {
        unstructuredNetwork = new HashSet<>();
        superPeerNetwork = new HashSet<>();
        assignedSuperPeersNetwork = new HashSet<>();
        nodes = new HashSet<>();

        unstructuredNetworkLock = new ReentrantReadWriteLock();
        superPeerNetworkLock = new ReentrantReadWriteLock();
        assignedSuperPeersNetworkLock = new ReentrantReadWriteLock();
        nodesLock = new ReentrantLock();
    }

    /**
     * Add a new connection to the unstructured network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean addUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2) {
        TraceableNode node1 = getNode(ip1, port1);
        node1.setState(NodeState.ACTIVE);
        TraceableNode node2 = getNode(ip2, port2);
        node2.setState(NodeState.ACTIVE);

        NetworkConnection connection = new NetworkConnection(node1, node2);
        boolean isSuccessful;
        unstructuredNetworkLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetwork.add(connection);
            if (isSuccessful) {
                logger.info("Added unstructured network connection " + connection.toString());
            } else {
                logger.info("Failed to add unstructured network connection " + connection.toString());
            }
        } finally {
            unstructuredNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Remove a new connection from the unstructured network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean removeUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2) {
        NetworkConnection connection = new NetworkConnection(getNode(ip1, port1), getNode(ip2, port2));
        boolean isSuccessful;
        unstructuredNetworkLock.writeLock().lock();
        try {
            isSuccessful = unstructuredNetwork.remove(connection);
            if (isSuccessful) {
                logger.info("Removed unstructured network connection " + connection.toString());
            } else {
                logger.info("Failed to remove unstructured network connection " + connection.toString());
            }
        } finally {
            unstructuredNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Add a new connection to the super peer network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean addSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2) {
        TraceableNode node1 = getNode(ip1, port1);
        node1.setState(NodeState.ACTIVE);
        TraceableNode node2 = getNode(ip2, port2);
        node2.setState(NodeState.ACTIVE);

        NetworkConnection connection = new NetworkConnection(node1, node2);
        boolean isSuccessful;
        superPeerNetworkLock.writeLock().lock();
        try {
            isSuccessful = superPeerNetwork.add(connection);
            if (isSuccessful) {
                logger.info("Added super peer network connection " + connection.toString());
            } else {
                logger.info("Failed to add super peer network connection " + connection.toString());
            }
        } finally {
            superPeerNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Removed a new connection from the super peer network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean removeSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2) {
        NetworkConnection connection = new NetworkConnection(getNode(ip1, port1), getNode(ip2, port2));
        boolean isSuccessful;
        superPeerNetworkLock.writeLock().lock();
        try {
            isSuccessful = superPeerNetwork.remove(connection);
            if (isSuccessful) {
                logger.info("Removed super peer network connection " + connection.toString());
            } else {
                logger.info("Failed to remove super peer network connection " + connection.toString());
            }
        } finally {
            superPeerNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Add a new connection to the assigned ordinary peer network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean addAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2) {
        TraceableNode node1 = getNode(ip1, port1);
        node1.setState(NodeState.ACTIVE);
        TraceableNode node2 = getNode(ip2, port2);
        node2.setState(NodeState.ACTIVE);

        NetworkConnection connection = new NetworkConnection(node1, node2);
        boolean isSuccessful;
        assignedSuperPeersNetworkLock.writeLock().lock();
        try {
            isSuccessful = assignedSuperPeersNetwork.add(connection);
            if (isSuccessful) {
                logger.info("Added assigned ordinary peer network connection " + connection.toString());
            } else {
                logger.info("Failed to add assigned ordinary peer network connection " + connection.toString());
            }
        } finally {
            assignedSuperPeersNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Removed a new connection from the super peer network.
     *
     * @param ip1   The ip of node1
     * @param port1 The port of node1
     * @param ip2   The ip of node2
     * @param port2 The port of node2
     * @return True of adding was successful
     */
    public boolean removeAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2) {
        NetworkConnection connection = new NetworkConnection(getNode(ip1, port1), getNode(ip2, port2));
        boolean isSuccessful;
        assignedSuperPeersNetworkLock.writeLock().lock();
        try {
            isSuccessful = assignedSuperPeersNetwork.remove(connection);
            if (isSuccessful) {
                logger.info("Removed assigned ordinary peer network connection " + connection.toString());
            } else {
                logger.info("Failed to remove assigned ordinary peer network connection " + connection.toString());
            }
        } finally {
            assignedSuperPeersNetworkLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Get a node from the network or a new node.
     *
     * @param ip   The ip of the node
     * @param port The port of the node
     * @return The required node
     */
    public TraceableNode getNode(String ip, int port) {
        TraceableNode node = getNodeFromNetworkConnectionsSet(ip, port, unstructuredNetwork, unstructuredNetworkLock);
        if (node == null) {
            node = getNodeFromNetworkConnectionsSet(ip, port, superPeerNetwork, superPeerNetworkLock);
        }
        if (node == null) {
            node = getNodeFromNetworkConnectionsSet(ip, port, assignedSuperPeersNetwork, assignedSuperPeersNetworkLock);
        }
        if (node == null) {
            nodesLock.lock();
            try {
                node = nodes.stream().parallel()
                        .filter(traceableNode -> traceableNode.equals(ip, port))
                        .findAny()
                        .orElse(null);
            } finally {
                nodesLock.unlock();
            }
        }
        if (node == null) {
            node = new TraceableNode(ip, port);
            nodesLock.lock();
            try {
                nodes.add(node);
            } finally {
                nodesLock.unlock();
            }
        }
        return node;
    }

    /**
     * Get the nodes in the network.
     *
     * @return The nodes in the network
     */
    public Set<TraceableNode> getNodes() {
        return new HashSet<>(nodes);
    }

    /**
     * Find a node from a set of network connections.
     * The lock is used to lock the list.
     * Returns null if not found.
     *
     * @param ip          The ip of the node to be found
     * @param port        The port of the node to be found
     * @param connections The set of conenctions from which the node will be found
     * @param lock        The lock to be used for locking the connections set
     * @return The required node
     */
    private TraceableNode getNodeFromNetworkConnectionsSet(String ip, int port, Set<NetworkConnection> connections,
                                                           ReadWriteLock lock) {
        TraceableNode node;
        lock.readLock().lock();
        try {
            node = connections.stream().parallel()
                    .flatMap(connection -> Stream.of(connection.getNode1(), connection.getNode2()))
                    .filter(existingTraceableNode -> existingTraceableNode.equals(ip, port))
                    .findAny()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
        return node;
    }
}
