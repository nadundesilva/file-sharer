package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.commons.messaging.Message;

/**
 * The base class for listeners waiting for router events.
 */
public interface RouterListener {
    /**
     * Invoked when a new message is received.
     *
     * @param message The message that was received
     */
    void onMessageReceived(Message message);
}
