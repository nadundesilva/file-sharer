package org.microfuse.file.sharer.node.commons.messaging;

/**
 * Message data indexes.
 */
public class MessageIndexes {
    // ECHO_OK message
    public static final int ECHO_OK_VALUE = 0;

    // REG message
    public static final int REG_IP = 0;
    public static final int REG_PORT = 1;
    public static final int REG_USERNAME = 2;

    // REG_OK message
    public static final int REG_OK_NODES_COUNT = 0;
    public static final int REG_OK_IP_PORT_START = 1;

    // UNREG message
    public static final int UNREG_IP = 0;
    public static final int UNREG_PORT = 1;
    public static final int UNREG_USERNAME = 2;

    // UNRREG_OK message
    public static final int UNREG_OK_VALUE = 0;

    // JOIN message
    public static final int JOIN_IP = 0;
    public static final int JOIN_PORT = 1;

    // JOIN_OK message
    public static final int JOIN_OK_VALUE = 0;
    public static final int JOIN_OK_IP = 1;
    public static final int JOIN_OK_PORT = 2;
    public static final int JOIN_OK_SUPER_PEER_IP = 3;
    public static final int JOIN_OK_SUPER_PEER_PORT = 4;

    // LEAVE message
    public static final int LEAVE_IP = 0;
    public static final int LEAVE_PORT = 1;

    // LEAVE_OK message
    public static final int LEAVE_OK_VALUE = 0;
    public static final int LEAVE_OK_IP = 1;
    public static final int LEAVE_OK_PORT = 2;

    // SER message
    public static final int SER_SOURCE_IP = 0;
    public static final int SER_SOURCE_PORT = 1;
    public static final int SER_FILE_NAME = 2;
    public static final int SER_HOP_COUNT = 3;

    // SER_OK message
    public static final int SER_OK_QUERY_STRING = 0;
    public static final int SER_OK_FILE_COUNT = 1;
    public static final int SER_OK_IP = 2;
    public static final int SER_OK_PORT = 3;
    public static final int SER_OK_FILE_NAME_START = 3;

    // SER_SUPER_PEER message
    public static final int SER_SUPER_PEER_SOURCE_IP = 0;
    public static final int SER_SUPER_PEER_SOURCE_PORT = 1;
    public static final int SER_SUPER_PEER_HOP_COUNT = 2;

    // SER_SUPER_PEER_OK message
    public static final int SER_SUPER_PEER_OK_IP = 0;
    public static final int SER_SUPER_PEER_OK_PORT = 1;

    // JOIN_SUPER_PEER
    public static final int JOIN_SUPER_PEER_SOURCE_TYPE = 0;
    public static final int JOIN_SUPER_PEER_SOURCE_IP = 1;
    public static final int JOIN_SUPER_PEER_SOURCE_PORT = 2;
    public static final int JOIN_SUPER_PEER_RESOURCE_COUNT = 3;
    public static final int JOIN_SUPER_PEER_RESOURCE_START_INDEX = 4;

    // JOIN_SUPER_PEER_OK message
    public static final int JOIN_SUPER_PEER_OK_VALUE = 0;
    public static final int JOIN_SUPER_PEER_OK_SOURCE_IP = 1;
    public static final int JOIN_SUPER_PEER_OK_SOURCE_PORT = 2;
    public static final int JOIN_SUPER_PEER_OK_NEW_IP = 3;
    public static final int JOIN_SUPER_PEER_OK_NEW_PORT = 4;

    // HEARTBEAT message
    public static final int HEARTBEAT_SOURCE_IP = 0;
    public static final int HEARTBEAT_SOURCE_PORT = 1;

    //HEARTBEAT_OK message
    public static final int HEARTBEAT_OK_IP = 0;
    public static final int HEARTBEAT_OK_PORT = 1;

    // LIST_RESOURCES message
    public static final int LIST_RESOURCES_IP = 0;
    public static final int LIST_RESOURCES_PORT = 1;

    // LIST_RESOURCES_OK message
    public static final int LIST_RESOURCES_OK_IP = 0;
    public static final int LIST_RESOURCES_OK_PORT = 1;
    public static final int LIST_RESOURCES_OK_RESOURCE_COUNT = 2;
    public static final int LIST_RESOURCES_OK_RESOURCE_START_INDEX = 3;

    // LIST_UNSTRUCTURED_CONNECTIONS message
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_IP = 0;
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_PORT = 1;

    // LIST_UNSTRUCTURED_CONNECTIONS_OK message
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_OK_IP = 0;
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_OK_PORT = 1;
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_COUNT = 2;
    public static final int LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_START_INDEX = 3;

    // LIST_SUPER_PEER_CONNECTIONS message
    public static final int LIST_SUPER_PEER_CONNECTIONS_IP = 0;
    public static final int LIST_SUPER_PEER_CONNECTIONS_PORT = 1;

    // LIST_SUPER_PEER_CONNECTIONS_OK message
    public static final int LIST_SUPER_PEER_CONNECTIONS_OK_IP = 0;
    public static final int LIST_SUPER_PEER_CONNECTIONS_OK_PORT = 1;
    public static final int LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_COUNT = 2;
    public static final int LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_START_INDEX = 3;

    private MessageIndexes() {   // Preventing from being initiated
    }
}
