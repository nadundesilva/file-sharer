package org.microfuse.node.core.communication.routing;

/**
 * The base class for listeners waiting for router events.
 */
public interface RouterListener {
    /**
     * Invoked when a new message is received.
     *
     * @param message The message that was received
     */
    void onMessageReceived(String message);
}
