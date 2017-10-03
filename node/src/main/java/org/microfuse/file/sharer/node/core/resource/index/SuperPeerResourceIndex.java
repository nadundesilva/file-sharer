package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resource Index for Super peers.
 *
 * Indexes resources owned by this node.
 * Indexes resources owned by the assigned ordinary peers.
 */
public class SuperPeerResourceIndex extends ResourceIndex {
    private Set<AggregatedResource> aggregatedResources;

    public SuperPeerResourceIndex() {
        aggregatedResources = new HashSet<>();
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resourceName The resource to be added
     */
    public void addResourceToAggregatedIndex(String resourceName, Node node) {
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem == null) {
            resourceIndexItem = new AggregatedResource();
            resourceIndexItem.setName(resourceName);
            aggregatedResources.add(resourceIndexItem);
        }
        resourceIndexItem.addNode(node);
    }

    /**
     * Remove entry from the aggregated resource index.
     *
     * @param resourceName The resource to be removed
     * @param node         The node which contains the resource
     */
    public void removeResourceToAggregatedIndex(String resourceName, Node node) {
        AggregatedResource resourceIndexItem = getAggregatedResource(resourceName);
        if (resourceIndexItem != null) {
            resourceIndexItem.removeNode(node);
            if (resourceIndexItem.getNodeCount() == 0) {
                aggregatedResources.remove(resourceIndexItem);
            }
        }
    }

    /**
     * Find all the resources in the aggregated resource index.
     *
     * @param resourceName The name of the resource
     * @return The list of resources matching the resource name
     */
    public Set<AggregatedResource> findAggregatedResources(String resourceName) {
        return matchResourcesWithName(
                aggregatedResources.stream().parallel()
                        .map(resource -> (Resource) resource)
                        .collect(Collectors.toSet()),
                resourceName
        ).stream().parallel()
                .map(resource -> (AggregatedResource) resource)
                .collect(Collectors.toSet());
    }

    /**
     * Get an aggregated resource by the name.
     * The name queried and the name of the resource returned will be equal.
     *
     * @param resourceName The name of the resource that should be retrieved
     * @return The aggregated resource
     */
    private AggregatedResource getAggregatedResource(String resourceName) {
        return aggregatedResources.stream().parallel()
                .filter(resource -> Objects.equals(resource.getName(), resourceName))
                .findAny()
                .orElse(null);
    }
}
