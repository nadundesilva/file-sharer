package org.microfuse.file.sharer.node.commons.tracing;

/**
 * Tracing modes used by nodes.
 */
public enum  TracingMode {
    TRACEABLE("Traceable"),
    TRACER("Tracer"),
    OFF("Off");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    TracingMode(String value) {
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
