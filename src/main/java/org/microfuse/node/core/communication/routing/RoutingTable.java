package org.microfuse.node.core.communication.routing;

import org.microfuse.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The routing table containing the node information.
 *
 * Contains the connections and the previous node in paths the message travels.
 */
public class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    /*
     * The routing tables for messages finding the destination
     */
    private Map<Integer, Node> idToNodeMap;
    private Map<String, Node> addressToNodeMap;

    /*
     * The routing tables for routing messages back to the source
     * Only the previous node in the path is kept for anonymity
     */
    private Map<Integer, Node> backwardRoutingTable;
    private Map<Integer, Integer> backwardRoutingMessageCount;

    public RoutingTable() {
        idToNodeMap = new HashMap<>();
        addressToNodeMap = new HashMap<>();
        backwardRoutingTable = new HashMap<>();
        backwardRoutingMessageCount = new HashMap<>();
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

    /**
     * Create an entry in the backward path routing table with the specified node.
     * A unique key will be selected which will allow the message to be routed back.
     *
     * @param node The node to be added to the path routing table
     * @return The key used in the path routing table
     */
    public int putBackwardRoutingTableEntry(Node node) {
        int pathKey = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        while (backwardRoutingTable.containsKey(pathKey)) {
            pathKey = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        incrementBackwardRoutingMessageCount(pathKey, 1);
        backwardRoutingTable.put(pathKey, node);
        return pathKey;
    }

    /**
     * Get a node using a path key from the backward path routing table.
     *
     * @param pathKey The unique path key used for routing
     * @return The node in the path
     */
    public Node removeBackwardRoutingTableEntry(int pathKey) {
        incrementBackwardRoutingMessageCount(pathKey, -1);
        return backwardRoutingTable.remove(pathKey);
    }

    /**
     * Increment the backward routing messages count.
     *
     * @param pathKey   The path key of backward routing
     * @param increment The increment to be done
     * @return The count after incrementing
     */
    public int incrementBackwardRoutingMessageCount(int pathKey, int increment) {
        Integer count = backwardRoutingMessageCount.computeIfAbsent(pathKey, k -> 0) + increment;
        backwardRoutingMessageCount.put(pathKey, count);
        return count;
    }
}
