package org.microfuse.file.sharer.node.core.resource;

import java.io.File;
import java.util.Objects;

/**
 * Resources stored by this node
 */
public class Resource {
    private String name;
    private File file;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
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
        return name.hashCode();
    }
}
