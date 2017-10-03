package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.commons.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * Resources stored by the ordinary peers assigned to this super peer node.
 */
public class AggregatedResource extends Resource {
    private Set<Node> nodes;

    public AggregatedResource() {
        nodes = new HashSet<>();
    }

    /**
     * Put a new entry into the nodes list.
     *
     * @param node The node which contains the resource
     */
    public void addNode(Node node) {
        nodes.add(node);

    }

    /**
     * Remove entry from the nodes list.
     *
     * @param node The node which contains the resource
     */
    public void removeNode(Node node) {
        nodes.remove(node);
    }

    /**
     * Get all the nodes containing this resource.
     *
     * @return The nodes containing this resource
     */
    public Set<Node> getAllNodes() {
        return new HashSet<>(nodes);
    }

    /**
     * Get the number of nodes containing this resource.
     *
     * @return The number of resources containing this resource
     */
    public int getNodeCount() {
        return nodes.size();
    }
}
