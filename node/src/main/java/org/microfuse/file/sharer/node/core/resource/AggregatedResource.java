package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.commons.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * Resources stored by the ordinary peers assigned to this super peer node.
 */
public class AggregatedResource extends Resource {
    private Set<Node> nodes;

    private final Object nodesKey;

    public AggregatedResource(String name) {
        super(name);
        nodesKey = new Object();
        nodes = new HashSet<>();
    }

    /**
     * Put a new entry into the nodes list.
     *
     * @param node The node which contains the resource
     */
    public boolean addNode(Node node) {
        synchronized (nodesKey) {
            return nodes.add(node);
        }

    }

    /**
     * Remove entry from the nodes list.
     *
     * @param node The node which contains the resource
     */
    public boolean removeNode(Node node) {
        synchronized (nodesKey) {
            return nodes.remove(node);
        }
    }

    /**
     * Get all the nodes containing this resource.
     *
     * @return The nodes containing this resource
     */
    public Set<Node> getAllNodes() {
        synchronized (nodesKey) {
            return new HashSet<>(nodes);
        }
    }

    /**
     * Get the number of nodes containing this resource.
     *
     * @return The number of resources containing this resource
     */
    public int getNodeCount() {
        synchronized (nodesKey) {
            return nodes.size();
        }
    }
}
