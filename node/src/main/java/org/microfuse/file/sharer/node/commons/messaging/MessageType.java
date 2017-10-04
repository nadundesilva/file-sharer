package org.microfuse.file.sharer.node.commons.messaging;

import java.util.HashMap;
import java.util.Map;

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
     * Used to store a map from values to MessageType.
     */
    private static Map<String, MessageType> valuesMap;

    static {
        // Initializing values map
        valuesMap = new HashMap<>();
        for (MessageType messageType : MessageType.values()) {
            valuesMap.put(messageType.value, messageType);
        }
    }

    /**
     * Get the message type identifier.
     *
     * @return The message type identifier
     */
    public String getValue() {
        return value;
    }

    public static MessageType parseMessageType(String messageType) {
        return valuesMap.get(messageType);
    }
}
