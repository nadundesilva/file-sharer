package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The routing table containing the node information.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public abstract class RoutingTable {
    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private Set<Node> unstructuredNetworkNodes;
    private Node bootstrapServer;

    public RoutingTable() {
        unstructuredNetworkNodes = new HashSet<>();
    }

    /**
     * Get the bootstrap server node.
     *
     * @return The bootstrap server node
     */
    public Node getBootstrapServer() {
        return bootstrapServer;
    }

    /**
     * Set the bootstrap server node.
     *
     * @param bootstrapServer The boostrap server node
     */
    public void setBootstrapServer(Node bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void addUnstructuredNetworkRoutingTableEntry(Node node) {
        unstructuredNetworkNodes.add(node);
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeUnstructuredNetworkRoutingTableEntry(Node node) {
        unstructuredNetworkNodes.remove(node);
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
     * @param ip The ip address of the node
     * @return The Node
     */
    public Node getUnstructuredNetworkRoutingTableNode(String ip) {
        return unstructuredNetworkNodes.stream().parallel()
                .filter(node -> Objects.equals(node.getIp(), ip))
                .findAny()
                .orElse(null);
    }
}
