package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The routing table containing the node information for super peers.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class SuperPeerRoutingTable extends RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRoutingTable.class);

    private List<Node> superPeerNetworkNodes;
    private List<Node> assignedOrdinaryPeerNodes;

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
     * @param node   The node of the new entry
     */
    public void removeSuperPeerNetworkRoutingTableEntry(Node node) {
        superPeerNetworkNodes.remove(node);
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public List<Node> getAllSuperPeerNetworkRoutingTableNodes() {
        return new ArrayList<>(superPeerNetworkNodes);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip The ip address of the node
     * @return The Node
     */
    public Node getSuperPeerNetworkRoutingTableNode(String ip) {
        for (Node node : superPeerNetworkNodes) {
            if (Objects.equals(node.getIp(), ip)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node   The node of the new entry
     */
    public void addAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        assignedOrdinaryPeerNodes.add(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node   The node of the new entry
     */
    public void removeAssignedOrdinaryNetworkRoutingTableEntry(Node node) {
        assignedOrdinaryPeerNodes.remove(node);
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public List<Node> getAllAssignedOrdinaryNetworkRoutingTableNodes() {
        return new ArrayList<>(assignedOrdinaryPeerNodes);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip The ip address of the node
     * @return The Node
     */
    public Node getAssignedOrdinaryNetworkRoutingTableNode(String ip) {
        for (Node node : assignedOrdinaryPeerNodes) {
            if (Objects.equals(node.getIp(), ip)) {
                return node;
            }
        }
        return null;
    }
}
