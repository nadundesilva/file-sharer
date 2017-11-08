package org.microfuse.file.sharer.node.commons.communication.messaging;

/**
 * UDP message type enum.
 */
public enum UDPMessageType {
    DATA("Data"),
    DATA_ACK("Data Acknowledgement"),
    ERROR("Error");

    /**
     * Constructor setting the UDP message identifier.
     *
     * @param value The UDP message identifier
     */
    UDPMessageType(String value) {
        this.value = value;
    }

    /**
     * Used to store the UDP message type identifier.
     */
    private String value;

    /**
     * Get the UDP message type identifier.
     *
     * @return The UDP message type identifier
     */
    public String getValue() {
        return value;
    }
}
