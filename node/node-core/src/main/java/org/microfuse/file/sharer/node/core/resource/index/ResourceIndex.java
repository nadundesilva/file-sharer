package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private static Map<PeerType, Class<? extends ResourceIndex>> resourceIndexClassMap;

    private Set<OwnedResource> ownedResources;

    private final ReadWriteLock ownedResourcesLock;

    protected ServiceHolder serviceHolder;

    static {
        // Populating the resource index class map
        resourceIndexClassMap = new HashMap<>();
        resourceIndexClassMap.put(PeerType.ORDINARY_PEER, ResourceIndex.class);
        resourceIndexClassMap.put(PeerType.SUPER_PEER, SuperPeerResourceIndex.class);
    }

    public ResourceIndex(ServiceHolder serviceHolder) {
        ownedResourcesLock = new ReentrantReadWriteLock();
        ownedResources = new HashSet<>();
        this.serviceHolder = serviceHolder;
    }

    public ResourceIndex(ServiceHolder serviceHolder, SuperPeerResourceIndex superPeerResourceIndex) {
        this(serviceHolder);
        superPeerResourceIndex.getAllOwnedResources().forEach(this::addOwnedResource);
    }

    /**
     * Get the resource index class based on the peer type.
     *
     * @param peerType The peer type
     * @return The resource index class
     */
    public static Class<? extends ResourceIndex> getResourceIndexClass(PeerType peerType) {
        return resourceIndexClassMap.get(peerType);
    }

    /**
     * Put a new entry into the resource index.
     *
     * @param resourceName The name of the resource to be added
     * @param storedFile The file which has the resource stored
     * @return True if adding was successful
     */
    public boolean addOwnedResource(String resourceName, File storedFile) {
        boolean isSuccessful;
        ownedResourcesLock.writeLock().lock();
        try {
            OwnedResource existingOwnedResource = ownedResources.stream().parallel()
                    .filter(ownedResource -> ownedResource.getName().equals(resourceName))
                    .findAny()
                    .orElse(null);

            if (existingOwnedResource != null) {
                logger.info("Resource " + resourceName + " already exists in owned resources.");
                existingOwnedResource.setFile(storedFile);
                isSuccessful = true;
            } else {
                OwnedResource ownedResource = new OwnedResource(resourceName);
                ownedResource.setFile(storedFile);
                isSuccessful = addOwnedResource(ownedResource);
            }

            if (isSuccessful) {
                logger.info("Added resource " + resourceName + " to owned resources.");
            } else {
                logger.info("Failed to add resource " + resourceName + " to owned resources.");
            }
        } finally {
            ownedResourcesLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resourceName The name of the resource to be removed
     * @return True if removing was successful
     */
    public boolean removeOwnedResource(String resourceName) {
        boolean isSuccessful;
        ownedResourcesLock.writeLock().lock();
        try {
            OwnedResource existingOwnedResource = ownedResources.stream().parallel()
                    .filter(ownedResource -> resourceName.equals(ownedResource.getName()))
                    .findAny()
                    .orElse(null);

            isSuccessful = ownedResources.remove(existingOwnedResource);
            if (isSuccessful) {
                logger.info("Removed resource " + resourceName + " from owned resources");
            } else {
                logger.info("Failed to remove resource " + resourceName + " from owned resources");
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
    public Set<OwnedResource> getAllOwnedResources() {
        return new HashSet<>(ownedResources);
    }

    /**
     * Find all the ownedResources in the resource index.
     *
     * @param resourceName The name of the resource
     * @return The list of ownedResources matching the resource name
     */
    public Set<OwnedResource> findOwnedResources(String resourceName) {
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
            logger.info("Cleared owned resources.");
        } finally {
            ownedResourcesLock.writeLock().unlock();
        }
    }

    /**
     * Collect garbage in the resource index.
     */
    public void collectGarbage() {
        logger.info("Skipping garbage collection in owned resources");
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

    /**
     * Add owned resource.
     *
     * @param ownedResource The owned resource to be added.
     * @return True if adding was successful
     */
    protected boolean addOwnedResource(OwnedResource ownedResource) {
        return ownedResources.add(ownedResource);
    }
}
