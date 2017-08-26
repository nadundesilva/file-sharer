package org.microfuse.node.commons.messaging;

/**
 * Message type enum.
 */
public enum MessageType {
    REQUEST,                // Request type message
    ACKNOWLEDGEMENT,        // Response to a request type message
    DESTINATION_NOT_FOUND   // Unable to reach the destination of the request type message
}
