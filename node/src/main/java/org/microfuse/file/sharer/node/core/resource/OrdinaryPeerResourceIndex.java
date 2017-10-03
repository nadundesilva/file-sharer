package org.microfuse.file.sharer.node.core.resource;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resource index for resource peer
 *
 * Stores the resources in the current node
 */
public class OrdinaryPeerResourceIndex {
    private Set<Resource> resources;

    public OrdinaryPeerResourceIndex() {
        resources = new HashSet<>();
    }

    /**
     * Put a new entry into the resource index.
     *
     * @param resource The resource to be added
     */
    public void addResourceToIndex(Resource resource) {
        resources.add(resource);
    }

    /**
     * Remove entry from the resource index.
     *
     * @param resource The resource to be removed
     */
    public void removeResourceToIndex(Resource resource) {
        resources.remove(resource);
    }

    /**
     * Find all the resources in the resource index
     *
     * @param resourceName The name of the resource
     * @return The list of resources matching the resource name
     */
    public Set<Resource> findResources(String resourceName) {
        return new HashSet<>(matchResourcesWithName(resources, resourceName));
    }

    /**
     * Match the resource with a resource name
     * Returns the list of resources that match the file name
     *
     * @param resources    The list of resource to search in
     * @param resourceName The name of the resource
     * @return Returns true if the resource and the resource name match
     */
    protected List<Resource> matchResourcesWithName(Collection<Resource> resources, String resourceName) {
        return resources.stream().parallel()
                .filter(resource -> {
                    Pattern pattern = Pattern.compile("(\\w|^)" + resourceName + "(\\s|$)");
                    Matcher matcher = pattern.matcher(resource.getName());
                    return matcher.find();
                })
                .collect(Collectors.toList());
    }
}
