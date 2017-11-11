package org.microfuse.file.sharer.node.commons.communication.messaging;

/**
 * Message related constants.
 */
public class MessageConstants {
    public static final String ECHO_OK_VALUE_SUCCESS = "0";

    public static final String JOIN_OK_VALUE_SUCCESS = "0";
    public static final String JOIN_OK_VALUE_ERROR = "9999";

    public static final String LEAVE_OK_VALUE_SUCCESS = "0";
    public static final String LEAVE_OK_VALUE_ERROR = "9999";

    public static final String REG_OK_NODE_COUNT_VALUE_ERROR_FULL = "9996";
    public static final String REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_OCCUPIED = "9997";
    public static final String REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_REGISTERED = "9998";
    public static final String REG_OK_NODE_COUNT_VALUE_ERROR = "9999";

    public static final String UNREG_OK_VALUE_SUCCESS = "0";
    public static final String UNREG_OK_VALUE_ERROR = "9999";

    public static final String SER_SUPER_PEER_OK_NOT_FOUND_IP = "0.0.0.0";
    public static final String SER_SUPER_PEER_OK_NOT_FOUND_PORT = "0";

    public static final String JOIN_SUPER_PEER_OK_VALUE_SUCCESS = "0";
    public static final String JOIN_SUPER_PEER_OK_VALUE_ERROR_NOT_SUPER_PEER = "9997";
    public static final String JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL = "9998";
    public static final String JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NO_ONE_ELSE = "9999";

    public static final String LIST_SUPER_PEER_CONNECTIONS_NOT_SUPER_PEER = "9999";
    public static final int LIST_SUPER_PEER_CONNECTIONS_MAX_COUNT = 2;

    public static final int LIST_UNSTRUCTURED_CONNECTIONS_MAX_COUNT = 2;

    private MessageConstants() {   // Preventing from being initiated
    }
}
