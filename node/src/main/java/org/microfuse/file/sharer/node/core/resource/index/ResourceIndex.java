package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resource Index for Ordinary Peers
 *
 * Indexes resources owned by this node.
 */
public class ResourceIndex {
    private static final Logger logger = LoggerFactory.getLogger(ResourceIndex.class);

    private Set<OwnedResource> ownedResources;

    private final ReadWriteLock ownedResourcesLock;

    public ResourceIndex() {
        ownedResourcesLock = new ReentrantReadWriteLock();
        ownedResources = new HashSet<>();
    }

    /**
     * Put a new entry into the resource index.
     *
     * @param resource The resource to be added
     */
    public boolean addResourceToIndex(OwnedResource resource) {
        boolean isSuccessful;
        ownedResourcesLock.writeLock().lock();
        try {
            ownedResources.stream().parallel()
                    .filter(ownedResource -> ownedResource.equals(resource))
                    .findAny()
                    .ifPresent(ownedResource -> {
                        logger.debug("Resource " + resource.toString() + " already exists in owned resources.");
                        removeResourceFromIndex(resource);
                    });
            isSuccessful = ownedResources.add(resource);
            if (isSuccessful) {
                logger.debug("Added resource " + resource.toString() + " to owned resources.");
            } else {
                logger.debug("Failed to add resource " + resource.toString() + " to owned resources.");
            }
        } finally {
            ownedResourcesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Add all resources in a collection to the index.
     *
     * @param resources The resources to be added
     */
    public void addAllResourceToIndex(Collection<OwnedResource> resources) {
        resources.forEach(this::addResourceToIndex);
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resourceName The name of the resource to be removed
     */
    public boolean removeResourceFromIndex(String resourceName) {
        return removeResourceFromIndex(new OwnedResource(resourceName));
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resource The resource to be removed
     */
    public boolean removeResourceFromIndex(OwnedResource resource) {
        boolean isSuccessful;
        ownedResourcesLock.writeLock().lock();
        try {
            isSuccessful = ownedResources.remove(resource);
            if (isSuccessful) {
                logger.debug("Removed resource " + resource.toString() + " from owned resources");
            } else {
                logger.debug("Failed to remove resource " + resource.toString() + " from owned resources");
            }
        } finally {
            ownedResourcesLock.writeLock().unlock();
        }
        return isSuccessful;
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
        Set<OwnedResource> requestedResources;
        ownedResourcesLock.readLock().lock();
        try {
            requestedResources = matchResourcesWithName(
                    ownedResources.stream().parallel()
                            .map(resource -> (Resource) resource)
                            .collect(Collectors.toSet()),
                    resourceName
            ).stream().parallel()
                    .map(resource -> (OwnedResource) resource)
                    .collect(Collectors.toSet());
        } finally {
            ownedResourcesLock.readLock().unlock();
        }
        return requestedResources;
    }

    /**
     * Clear the resource index.
     */
    public void clear() {
        ownedResourcesLock.writeLock().lock();
        try {
            ownedResources.clear();
            logger.debug("Cleared owned resources.");
        } finally {
            ownedResourcesLock.writeLock().unlock();
        }
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
