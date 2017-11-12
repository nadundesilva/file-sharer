package org.microfuse.file.sharer.node.ui.backend.core.utils;

/**
 * Mode of the current file sharer.
 */
public enum FileSharerMode {
    FILE_SHARER("File Sharer"),
    TRACER("File Sharer Tracer");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    FileSharerMode(String value) {
        this.value = value;
    }

    /**
     * Get the value to be displayed.
     *
     * @return The value to be displayed
     */
    public String getValue() {
        return value;
    }
}
