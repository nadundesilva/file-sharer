package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Resource Index for Super peers.
 *
 * Indexes resources owned by this node.
 * Indexes resources owned by the assigned ordinary peers.
 */
public class SuperPeerResourceIndex extends ResourceIndex {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerResourceIndex.class);

    private Set<AggregatedResource> aggregatedResources;

    private final ReadWriteLock aggregatedResourcesLock;

    public SuperPeerResourceIndex() {
        aggregatedResourcesLock = new ReentrantReadWriteLock();
        aggregatedResources = new HashSet<>();
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resourceName The name of the resource to be added
     * @param node         The node which contains the resource to be added
     */
    public void addAggregatedResource(String resourceName, Node node) {
        boolean isSuccessful;
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem == null) {
            resourceIndexItem = new AggregatedResource(resourceName);

            aggregatedResourcesLock.writeLock().lock();
            try {
                isSuccessful = aggregatedResources.add(resourceIndexItem);
                if (isSuccessful) {
                    logger.debug("Added resource " + resourceIndexItem.toString() + " to aggregated resources.");
                } else {
                    logger.debug("Failed to add resource " + resourceIndexItem.toString()
                            + " to aggregated resources.");
                }
            } finally {
                aggregatedResourcesLock.writeLock().unlock();
            }
        }
        resourceIndexItem.addNode(node);
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resourceNames The names of the resources to be added
     * @param node          The node which contains the resources to be added
     */
    public void addAllAggregatedResources(Collection<String> resourceNames, Node node) {
        resourceNames.forEach(resourceName -> addAggregatedResource(resourceName, node));
    }

    /**
     * Remove entry from the aggregated resource index.
     *
     * @param resourceName The resource to be removed
     * @param node         The node which contains the resource
     */
    public boolean removeAggregatedResource(String resourceName, Node node) {
        boolean isSuccessful = false;
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem != null) {
            isSuccessful = resourceIndexItem.removeNode(node);
            if (resourceIndexItem.getNodeCount() == 0) {
                aggregatedResourcesLock.writeLock().lock();
                try {
                    if (aggregatedResources.remove(resourceIndexItem)) {
                        logger.debug("Removed resource " + resourceIndexItem.toString()
                                + " from aggregated resources since it does not have any more nodes.");
                    } else {
                        logger.debug("Failed to remove resource " + resourceIndexItem.toString()
                                + " from aggregated resources although it does not have any more nodes.");
                    }
                } finally {
                    aggregatedResourcesLock.writeLock().unlock();
                }
            }
        }
        return isSuccessful;
    }

    /**
     * Find all the resources in the aggregated resource index.
     *
     * @param resourceName The name of the resource
     * @return The list of resources matching the resource name
     */
    public Set<AggregatedResource> findAggregatedResources(String resourceName) {
        Set<AggregatedResource> requestedResource;
        aggregatedResourcesLock.readLock().lock();
        try {
            requestedResource = matchResourcesWithName(
                    aggregatedResources.stream().parallel()
                            .map(resource -> (Resource) resource)
                            .collect(Collectors.toSet()),
                    resourceName
            ).stream().parallel()
                    .map(resource -> (AggregatedResource) resource)
                    .collect(Collectors.toSet());
        } finally {
            aggregatedResourcesLock.readLock().unlock();
        }
        return requestedResource;
    }

    /**
     * Get all the aggregated resources in the index.
     *
     * @return The aggregated resources in the index.
     */
    public Set<AggregatedResource> getAllAggregatedResources() {
        return new HashSet<>(aggregatedResources);
    }

    /**
     * Clear the resource index.
     */
    public void clear() {
        super.clear();
        aggregatedResourcesLock.writeLock().lock();
        try {
            aggregatedResources.clear();
            logger.debug("Cleared aggregated resources.");
        } finally {
            aggregatedResourcesLock.writeLock().unlock();
        }
    }

    /**
     * Remove a node from the aggregated resources.
     *
     * @param node The node to be removed from the resource index
     */
    public void removeNodeFromAggregatedResources(Node node) {
        aggregatedResources.forEach(aggregatedResource -> aggregatedResource.getAllNodes().remove(node));
    }

    /**
     * Get an aggregated resource by the name.
     * The name queried and the name of the resource returned will be equal.
     *
     * @param resourceName The name of the resource that should be retrieved
     * @return The aggregated resource
     */
    private AggregatedResource getAggregatedResource(String resourceName) {
        AggregatedResource requestedResource;
        aggregatedResourcesLock.readLock().lock();
        try {
            requestedResource = aggregatedResources.stream().parallel()
                    .filter(resource -> Objects.equals(resource.getName(), resourceName))
                    .findAny()
                    .orElse(null);
        } finally {
            aggregatedResourcesLock.readLock().unlock();
        }
        return requestedResource;
    }
}
