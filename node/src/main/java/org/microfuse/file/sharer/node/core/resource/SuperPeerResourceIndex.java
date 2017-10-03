package org.microfuse.file.sharer.node.core.resource;

import java.util.ArrayList;
import java.util.List;

public class SuperPeerResourceIndex extends OrdinaryPeerResourceIndex {
    private List<Resource> aggregatedResources;

    public SuperPeerResourceIndex() {
        aggregatedResources = new ArrayList<>();
    }

    /**
     * Put a new entry into the aggregated resource index.
     *
     * @param resource The resource to be added
     */
    public void addResourceToAggregatedIndex(Resource resource) {
        aggregatedResources.add(resource);
    }

    /**
     * Remove entry from the aggregated resource index.
     *
     * @param resource The resource to be removed
     */
    public void removeResourceToAggregatedIndex(Resource resource) {
        aggregatedResources.remove(resource);
    }

    /**
     * Find all the resources in the aggregated resource index
     *
     * @param resourceName The name of the resource
     * @return The list of resources matching the resource name
     */
    public List<Resource> findAggregatedResources(String resourceName) {
        return matchResourcesWithName(aggregatedResources, resourceName);
    }
}
