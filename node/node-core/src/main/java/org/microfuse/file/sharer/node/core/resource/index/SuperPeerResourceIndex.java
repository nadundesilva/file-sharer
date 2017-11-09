package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
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

    public SuperPeerResourceIndex(ServiceHolder serviceHolder) {
        super(serviceHolder);
        aggregatedResourcesLock = new ReentrantReadWriteLock();
        aggregatedResources = new HashSet<>();
    }

    public SuperPeerResourceIndex(ServiceHolder serviceHolder, ResourceIndex resourceIndex) {
        this(serviceHolder);
        resourceIndex.getAllOwnedResources().forEach(this::addOwnedResource);
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resourceName The name of the resource to be added
     * @param ip           The ip of the node which contains the resource to be added
     * @param port         The port of the node which contains the resource to be added
     */
    public void addAggregatedResource(String resourceName, String ip, int port) {
        boolean isSuccessful;
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem == null) {
            resourceIndexItem = new AggregatedResource(resourceName);

            aggregatedResourcesLock.writeLock().lock();
            try {
                isSuccessful = aggregatedResources.add(resourceIndexItem);
                if (isSuccessful) {
                    logger.info("Added resource " + resourceIndexItem.toString() + " to aggregated resources.");
                } else {
                    logger.info("Failed to add resource " + resourceIndexItem.toString()
                            + " to aggregated resources.");
                }
            } finally {
                aggregatedResourcesLock.writeLock().unlock();
            }
        }

        Node node = serviceHolder.getRouter().getRoutingTable().get(ip, port);
        if (node != null) {
            aggregatedResourcesLock.readLock().lock();
            try {
                resourceIndexItem.addNode(node);
            } finally {
                aggregatedResourcesLock.readLock().unlock();
            }
        } else {
            logger.info("Request to add resource " + resourceName + " from unknown node "
                    + ip + ":" + port + " ignored");
        }
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resourceNames The names of the resources to be added
     * @param ip            The ip of the node which contains the resources to be added
     * @param port          The port of the node which contains the resources to be added
     */
    public void addAllAggregatedResources(Collection<String> resourceNames, String ip, int port) {
        resourceNames.forEach(resourceName -> addAggregatedResource(resourceName, ip, port));
    }

    /**
     * Remove entry from the aggregated resource index.
     *
     * @param resourceName The resource to be removed
     * @param ip           The ip of the node which contains the resource
     * @param port         The port of the node which contains the resource
     */
    public boolean removeAggregatedResource(String resourceName, String ip, int port) {
        boolean isSuccessful = false;
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem != null) {
            Node node = serviceHolder.getRouter().getRoutingTable().get(ip, port);
            if (node != null) {
                isSuccessful = resourceIndexItem.removeNode(node);
                removeEmptyAggregatedResources();
            } else {
                logger.info("Request to remove resource " + resourceName + " from unknown node "
                        + ip + ":" + port + " ignored");
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
            logger.info("Cleared aggregated resources.");
        } finally {
            aggregatedResourcesLock.writeLock().unlock();
        }
    }

    /**
     * Remove a node from the aggregated resources.
     *
     * @param ip   The ip of the node to be removed from the resource index
     * @param port The port of the node to be removed from the resource index
     */
    public void removeNodeFromAggregatedResources(String ip, int port) {
        Node node = serviceHolder.getRouter().getRoutingTable().get(ip, port);
        if (node != null) {
            aggregatedResourcesLock.readLock().lock();
            try {
                aggregatedResources.forEach(aggregatedResource -> aggregatedResource.removeNode(node));
            } finally {
                aggregatedResourcesLock.readLock().unlock();
            }
            removeEmptyAggregatedResources();
        } else {
            logger.warn("Request to remove unknown node " + ip + ":" + port + " ignored.");
        }
    }

    @Override
    public void collectGarbage() {
        super.collectGarbage();
        Set<Node> garbageNodes = aggregatedResources.stream().parallel()
                .flatMap(aggregatedResource ->
                        aggregatedResource.getAllNodes().stream().parallel()
                                .filter(node -> !node.isActive())
                ).collect(Collectors.toSet());

        garbageNodes.forEach(node -> {
            logger.info("Removed inactive node " + node.toString() + " from the routing table");
            removeNodeFromAggregatedResources(node.getIp(), node.getPort());
        });
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

    /**
     * Remove the aggregated resources which do not have any nodes.
     */
    private void removeEmptyAggregatedResources() {
        aggregatedResourcesLock.writeLock().lock();
        try {
            Set<AggregatedResource> emptyResources = aggregatedResources.stream().parallel()
                    .filter(aggregatedResource -> aggregatedResource.getNodeCount() == 0)
                    .collect(Collectors.toSet());

            emptyResources.forEach(aggregatedResource -> {
                if (aggregatedResources.remove(aggregatedResource)) {
                    logger.info("Removed resource " + aggregatedResource.toString()
                            + " from aggregated resources since it does not have any more nodes.");
                } else {
                    logger.info("Failed to remove resource " + aggregatedResource.toString()
                            + " from aggregated resources although it does not have any more nodes.");
                }
            });
        } finally {
            aggregatedResourcesLock.writeLock().unlock();
        }
    }
}
