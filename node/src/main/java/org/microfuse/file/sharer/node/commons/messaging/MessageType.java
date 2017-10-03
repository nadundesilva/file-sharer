package org.microfuse.file.sharer.node.commons.messaging;

/**
 * Message type enum.
 */
public enum MessageType {
    REG("REG"),
    REG_OK("REGOK"),
    UNREG("UNREG"),
    UNREG_OK("UNROK"),
    JOIN("JOIN"),
    JOIN_OK("JOINOK"),
    LEAVE("LEAVE"),
    LEAVE_OK("LEAVEOK"),
    SER("SER"),
    SER_OK("SEROK"),
    ERROR("ERROR"),

    // Only used by the bootstrap server
    ECHO("ECHO"),
    ECHO_OK("ECHOK");

    /**
     * Constructor setting the message identifier.
     *
     * @param value The message identifier
     */
    MessageType(String value) {
        this.value = value;
    }

    /**
     * Used to store the message type identifier.
     */
    private String value;

    /**
     * Get the message type identifier.
     *
     * @return The message type identifier
     */
    public String getValue() {
        return value;
    }
}
