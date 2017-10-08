package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;

/**
 * The network handler listener.
 * <p>
 * Classes extending this can be registered to listen to messages received.
 */
public interface NetworkHandlerListener {
    /**
     * Invoked when a new message is received.
     *
     * @param fromAddress The address from which the message was received
     * @param fromPort    The port from which the message was received
     * @param message     The message received
     */
    void onMessageReceived(String fromAddress, int fromPort, Message message);

    /**
     * Invoked when an error is occurred in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param toPort    The port to which the message should be sent
     * @param message   The message involved with the error
     */
    void onMessageSendFailed(String toAddress, int toPort, Message message);
}
