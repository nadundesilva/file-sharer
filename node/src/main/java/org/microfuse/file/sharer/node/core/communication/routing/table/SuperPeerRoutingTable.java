package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The routing table containing the node information for super peers.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class SuperPeerRoutingTable extends RoutingTable {
    private Set<Node> superPeerNetworkNodes;
    private Set<Node> assignedOrdinaryPeerNodes;

    private final Object superPeerNetworkNodesKey;
    private final Object assignedOrdinaryPeerNodesKey;

    public SuperPeerRoutingTable() {
        superPeerNetworkNodesKey = new Object();
        assignedOrdinaryPeerNodesKey = new Object();
        superPeerNetworkNodes = new HashSet<>();
        assignedOrdinaryPeerNodes = new HashSet<>();
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node   The node of the new entry
     */
    public void addSuperPeerNetworkRoutingTableEntry(Node node) {
        synchronized (superPeerNetworkNodesKey) {
            superPeerNetworkNodes.add(node);
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeSuperPeerNetworkRoutingTableEntry(Node node) {
        synchronized (superPeerNetworkNodesKey) {
            superPeerNetworkNodes.remove(node);
        }
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllSuperPeerNetworkRoutingTableNodes() {
        synchronized (superPeerNetworkNodesKey) {
            return new HashSet<>(superPeerNetworkNodes);
        }
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getSuperPeerNetworkRoutingTableNode(String ip, int port) {
        synchronized (superPeerNetworkNodesKey) {
            return superPeerNetworkNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && node.getPort() == port)
                    .findAny()
                    .orElse(null);
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void addAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        synchronized (assignedOrdinaryPeerNodesKey) {
            assignedOrdinaryPeerNodes.add(node);
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        synchronized (assignedOrdinaryPeerNodesKey) {
            assignedOrdinaryPeerNodes.remove(node);
        }
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllAssignedOrdinaryNetworkRoutingTableNodes() {
        synchronized (assignedOrdinaryPeerNodesKey) {
            return new HashSet<>(assignedOrdinaryPeerNodes);
        }
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getAssignedOrdinaryNetworkRoutingTableNode(String ip, int port) {
        synchronized (assignedOrdinaryPeerNodesKey) {
            return assignedOrdinaryPeerNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && Objects.equals(node.getPort(), port))
                    .findAny()
                    .orElse(null);
        }
    }
}
