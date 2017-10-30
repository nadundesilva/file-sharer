package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resources stored by the ordinary peers assigned to this super peer node.
 */
public class AggregatedResource extends Resource {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedResource.class);

    private Set<Node> nodes;

    private final ReadWriteLock nodeLock;

    public AggregatedResource(String name) {
        super(name);
        nodeLock = new ReentrantReadWriteLock();
        nodes = new HashSet<>();
    }

    /**
     * Put a new entry into the nodes list.
     *
     * @param node The node which contains the resource
     * @return True if adding was successful
     */
    public boolean addNode(Node node) {
        boolean isSuccessful;
        nodeLock.writeLock().lock();
        try {
            isSuccessful = nodes.add(node);
            if (isSuccessful) {
                logger.debug("Added node " + node.toString() + " to aggregated resource " + toString());
            } else {
                logger.debug("Failed to add node " + node.toString() + " to aggregated resource " + toString());
            }
        } finally {
            nodeLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Remove entry from the nodes list.
     *
     * @param node The node which contains the resource
     * @return True if removing was successful
     */
    public boolean removeNode(Node node) {
        boolean isSuccessful;
        nodeLock.writeLock().lock();
        try {
            isSuccessful = nodes.remove(node);
            if (isSuccessful) {
                logger.debug("Removed node " + node.toString() + " from aggregated resource " + toString());
            } else {
                logger.debug("Failed to remove node " + node.toString() + " from aggregated resource " + toString());
            }
        } finally {
            nodeLock.writeLock().unlock();
        }
        return isSuccessful;
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
        int nodesCount;
        nodeLock.readLock().lock();
        try {
            nodesCount = nodes.size();
        } finally {
            nodeLock.readLock().unlock();
        }
        return nodesCount;
    }
}
