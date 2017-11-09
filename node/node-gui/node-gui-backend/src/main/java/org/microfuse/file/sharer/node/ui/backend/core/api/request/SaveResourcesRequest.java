package org.microfuse.file.sharer.node.ui.backend.core.api.request;

import java.util.List;

/**
 * Save resources request POJO.
 */
public class SaveResourcesRequest {
    private List<String> resourceNames;

    public List<String> getResourceNames() {
        return resourceNames;
    }

    public void setResourceNames(List<String> resourceNames) {
        this.resourceNames = resourceNames;
    }
}
