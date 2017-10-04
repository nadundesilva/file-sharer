package org.microfuse.file.sharer.node.core.resource;

import java.io.File;

/**
 * Resources stored by this node.
 */
public class OwnedResource extends Resource {
    private File file;

    public OwnedResource(String name) {
        super(name);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
