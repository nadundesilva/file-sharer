package org.microfuse.file.sharer.node.core.resource;

import java.util.Objects;

/**
 * Resource base abstract class.
 */
public abstract class Resource {
    private String name;

    public Resource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof Resource) {
            Resource nodeObject = (Resource) object;
            return Objects.equals(nodeObject.getName(), this.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (name != null) {
            return name.hashCode();
        } else {
            return "".hashCode();
        }
    }
}
