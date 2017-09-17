package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The routing table containing the node information.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    /*
     * The routing tables for messages finding the destination
     */
    private Map<Integer, Node> idToNodeMap;
    private Map<String, Node> addressToNodeMap;

    public RoutingTable() {
        idToNodeMap = new HashMap<>();
        addressToNodeMap = new HashMap<>();
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param nodeID The ID of the node
     * @param node   The node of the new entry
     */
    public void addRoutingTableEntry(int nodeID, String address, Node node) {
        idToNodeMap.put(nodeID, node);
        addressToNodeMap.put(address, node);
        logger.debug("Added node " + nodeID + " to the routing table");
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param nodeID The ID of the node
     * @param node   The node of the new entry
     */
    public void removeRoutingTableEntry(int nodeID, String address, Node node) {
        idToNodeMap.remove(nodeID, node);
        addressToNodeMap.remove(address, node);
        logger.debug("Removed node " + nodeID + " from the routing table");
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public List<Node> getAllRoutingTableNodes() {
        return new ArrayList<>(idToNodeMap.values());
    }

    /**
     * Get a node from the routing table based on te node ID.
     *
     * @param nodeID The ID of the node
     * @return The Node
     */
    public Node getRoutingTableNode(int nodeID) {
        return idToNodeMap.get(nodeID);
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param address The address of the node
     * @return The Node
     */
    public Node getRoutingTableNode(String address) {
        return addressToNodeMap.get(address);
    }
}
