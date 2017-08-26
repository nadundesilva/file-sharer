package org.microfuse.node.core.communication.network;

/**
 * The network handler listener.
 *
 * Classes extending this can be registered to listen to messages received.
 */
public interface NetworkHandlerListener {
    /**
     * Invoked when a new message is received.
     *
     * @param fromAddress The address from which the message was received
     * @param message     The message received
     */
    void onMessageReceived(String fromAddress, String message);

    /**
     * Invoked when an error is occurred in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param message   The message involved with the error
     */
    void onMessageSendFailed(String toAddress, String message);
}
