package org.microfuse.file.sharer.node.core.utils;

/**
 * Message data indexes.
 */
public class MessageIndexes {
    // REG message
    public static final int REG_IP = 0;
    public static final int REG_PORT = 1;
    public static final int REG_USERNAME = 2;

    // REGOK message
    public static final int REGOK_NODES_COUNT = 0;
    public static final int REGOK_IP_PORT_START = 0;

    // UNREG message
    public static final int UNREG_IP = 0;
    public static final int UNREG_PORT = 1;
    public static final int UNREG_USERNAME = 2;

    // UNROK message
    public static final int UNROK_VALUE = 0;

    // JOIN message
    public static final int JOIN_IP = 0;
    public static final int JOIN_PORT = 1;

    // JOINOK message
    public static final int JOINOK_VALUE = 0;

    // LEAVE message
    public static final int LEAVE_IP = 0;
    public static final int LEAVE_PORT = 1;

    // LEAVEOK message
    public static final int LEAVEOK_VALUE = 0;

    // SER message
    public static final int SER_SOURCE_IP = 0;
    public static final int SER_SOURCE_PORT = 1;
    public static final int SER_FILE_NAME = 2;
    public static final int SER_HOP_COUNT = 3;

    // SEROK message
    public static final int SER_OK_FILE_COUNT = 0;
    public static final int SER_OK_SOURCE_IP = 1;
    public static final int SER_OK_SOURCE_PORT = 2;
    public static final int SER_OK_FILE_NAME_START = 3;

    private MessageIndexes() {   // Preventing from being initiated
    }
}
