package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resource Index for Ordinary Peers
 *
 * Indexes resources owned by this node.
 */
public class ResourceIndex {
    private Set<OwnedResource> ownedResources;

    public ResourceIndex() {
        ownedResources = new HashSet<>();
    }

    /**
     * Put a new entry into the resource index.
     *
     * @param resource The resource to be added
     */
    public void addResourceToIndex(OwnedResource resource) {
        ownedResources.stream().parallel()
                .filter(ownedResource -> ownedResource.equals(resource))
                .findAny()
                .ifPresent(ownedResource -> removeResourceFromIndex(resource));
        ownedResources.add(resource);
    }

    /**
     * Add all resources in a collection to the index.
     *
     * @param resources The resources to be added
     */
    public void addAllResourceToIndex(Collection<OwnedResource> resources) {
        for (OwnedResource resource : resources) {
            ownedResources.stream().parallel()
                    .filter(ownedResource -> ownedResource.equals(resource))
                    .findAny()
                    .ifPresent(ownedResource -> removeResourceFromIndex(resource));
            ownedResources.add(resource);
        }
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resourceName The name of the resource to be removed
     */
    public void removeResourceFromIndex(String resourceName) {
       removeResourceFromIndex(new OwnedResource(resourceName));
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resource The resource to be removed
     */
    public void removeResourceFromIndex(OwnedResource resource) {
        ownedResources.remove(resource);
    }

    /**
     * Get all the resources in this index.
     *
     * @return The resources in this index
     */
    public Set<OwnedResource> getAllResourcesInIndex() {
        return new HashSet<>(ownedResources);
    }

    /**
     * Find all the ownedResources in the resource index.
     *
     * @param resourceName The name of the resource
     * @return The list of ownedResources matching the resource name
     */
    public Set<OwnedResource> findResources(String resourceName) {
        return matchResourcesWithName(
                ownedResources.stream().parallel()
                        .map(resource -> (Resource) resource)
                        .collect(Collectors.toSet()),
                resourceName
        ).stream().parallel()
                .map(resource -> (OwnedResource) resource)
                .collect(Collectors.toSet());
    }

    /**
     * Clear the resource index.
     */
    public void clear() {
        ownedResources.clear();
    }

    /**
     * Match the resource with a resource name.
     * Returns the list of ownedResources that match the file name.
     *
     * @param resources    The list of resource to search in
     * @param resourceName The name of the resource
     * @return Returns true if the resource and the resource name match
     */
    protected Set<Resource> matchResourcesWithName(Collection<Resource> resources, String resourceName) {
        return resources.stream().parallel()
                .filter(resource -> {
                    Pattern pattern = Pattern.compile("(\\w|^)" + resourceName + "(\\s|$)");
                    Matcher matcher = pattern.matcher(resource.getName());
                    return matcher.find();
                })
                .collect(Collectors.toSet());
    }
}
