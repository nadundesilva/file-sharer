package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The routing table containing the node information.
 * <p>
 * Contains the connections and the previous node in paths the message travels.
 */
public abstract class RoutingTable {
    private Set<Node> unstructuredNetworkNodes;
    private Node bootstrapServer;

    private final Object unstructuredNetworkNodesKey;

    public RoutingTable() {
        unstructuredNetworkNodesKey = new Object();
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
        synchronized (unstructuredNetworkNodesKey) {
            unstructuredNetworkNodes.add(node);
        }
    }

    /**
     * Add all nodes into the unstructured network from a collection.
     *
     * @param nodes The nodes to be added
     */
    public void addAllUnstructuredNetworkRoutingTableEntry(Collection<Node> nodes) {
        synchronized (unstructuredNetworkNodesKey) {
            unstructuredNetworkNodes.addAll(nodes);
        }
    }

    /**
     * Put a new entry into the routing table of this router.
     *
     * @param node The node of the new entry
     */
    public void removeUnstructuredNetworkRoutingTableEntry(Node node) {
        synchronized (unstructuredNetworkNodesKey) {
            unstructuredNetworkNodes.remove(node);
        }
    }

    /**
     * Get all the node in the routing table.
     *
     * @return The list of nodes in the routing table
     */
    public Set<Node> getAllUnstructuredNetworkRoutingTableNodes() {
        synchronized (unstructuredNetworkNodesKey) {
            return new HashSet<>(unstructuredNetworkNodes);
        }
    }

    /**
     * Get a node from the routing table based on te node address.
     *
     * @param ip   The ip address of the node
     * @param port The port of the node
     * @return The Node
     */
    public Node getUnstructuredNetworkRoutingTableNode(String ip, int port) {
        synchronized (unstructuredNetworkNodesKey) {
            return unstructuredNetworkNodes.stream().parallel()
                    .filter(node -> Objects.equals(node.getIp(), ip) && node.getPort() == port)
                    .findAny()
                    .orElse(null);
        }
    }
}
