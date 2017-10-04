package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The routing table containing the node information for super peers.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class SuperPeerRoutingTable extends RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRoutingTable.class);

    private Set<Node> superPeerNetworkNodes;
    private Set<Node> assignedOrdinaryPeerNodes;

    public SuperPeerRoutingTable() {
        superPeerNetworkNodes = new HashSet<>();
        assignedOrdinaryPeerNodes = new HashSet<>();
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node   The node of the new entry
     */
    public void addSuperPeerNetworkRoutingTableEntry(Node node) {
        superPeerNetworkNodes.add(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeSuperPeerNetworkRoutingTableEntry(Node node) {
        superPeerNetworkNodes.remove(node);
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
        return superPeerNetworkNodes.stream().parallel()
                .filter(node -> Objects.equals(node.getIp(), ip) && node.getPort() == port)
                .findAny()
                .orElse(null);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void addAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        assignedOrdinaryPeerNodes.add(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        assignedOrdinaryPeerNodes.remove(node);
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
        return assignedOrdinaryPeerNodes.stream().parallel()
                .filter(node -> Objects.equals(node.getIp(), ip) && Objects.equals(node.getPort(), port))
                .findAny()
                .orElse(null);
    }
}
