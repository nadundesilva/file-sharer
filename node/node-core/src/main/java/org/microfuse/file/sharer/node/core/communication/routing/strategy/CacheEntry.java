package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.peer.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Routing strategy cache entry.
 */
public class CacheEntry {
    private long timeStamp;
    private Set<Node> forwardedNodes;

    public CacheEntry() {
        timeStamp = 0;
        forwardedNodes = new HashSet<>();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Get all the forwarding nodes in the cache entry.
     *
     * @return The forwarded nodes in the entry
     */
    public Set<Node> getAllForwardedNodes() {
        return forwardedNodes;
    }

    /**
     * Add multiple nodes to the entry.
     *
     * @param nodesToBeAdded The nodes to be added
     * @return True is successful
     */
    public boolean addAllForwardedNodes(Collection<Node> nodesToBeAdded) {
        return this.forwardedNodes.addAll(nodesToBeAdded);
    }

    /**
     * Add a single node to the entry.
     *
     * @param nodeToBeAdded The node to be added
     * @return True is successful
     */
    public boolean addForwardedNode(Node nodeToBeAdded) {
        return this.forwardedNodes.add(nodeToBeAdded);
    }

    /**
     * Remove multiple nodes from the entry.
     *
     * @param nodesToBeRemoved The nodes to be removed
     * @return True is successful
     */
    public boolean removeForwardedNodes(Collection<Node> nodesToBeRemoved) {
        return this.forwardedNodes.removeAll(nodesToBeRemoved);
    }

    /**
     * Remove a single node from the entry.
     *
     * @param nodeToBeRemoved The node to be removed
     * @return True is successful
     */
    public boolean removeForwardedNode(Node nodeToBeRemoved) {
        return this.forwardedNodes.remove(nodeToBeRemoved);
    }
}
