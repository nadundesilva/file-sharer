package org.microfuse.file.sharer.node.commons.communication.network;

/**
 * Network handler types.
 */
public enum NetworkHandlerType {
    TCP_SOCKET("TCP Socket"),
    UDP_SOCKET("UDP Socket");

    /**
     * Contains the value to be displayed.
     */
    private String value;

    NetworkHandlerType(String value) {
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
