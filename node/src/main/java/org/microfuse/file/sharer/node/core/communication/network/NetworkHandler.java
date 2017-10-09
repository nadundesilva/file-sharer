package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Network Handler SuperClass.
 * <p>
 * All types of network handlers should extend this abstract class.
 */
public abstract class NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

    private List<NetworkHandlerListener> listenersList;

    private final Object listenersListKey;

    public NetworkHandler() {
        listenersListKey = new Object();
        listenersList = new ArrayList<>();
    }

    /**
     * Get the name of the network handler.
     *
     * @return The name of the network handler
     */
    public abstract String getName();

    /**
     * Start listening to messages from other devices.
     */
    public abstract void startListening();

    /**
     * Send a message to the specified node.
     *
     * @param ip      The ip address to which the message should be sent
     * @param port    The port to which the message should be sent
     * @param message The message to be sent
     */
    public abstract void sendMessage(String ip, int port, Message message);

    /**
     * Runs tasks to be run when an error occurs in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param toPort    The port to which the message should be sent
     * @param message   The message
     */
    protected void onMessageSendFailed(String toAddress, int toPort, Message message) {
        logger.debug("Failed to send message to " + toAddress + ": " + message);
        synchronized (listenersListKey) {
            listenersList.forEach(listener -> listener.onMessageSendFailed(toAddress, toPort, message));
        }
    }

    /**
     * Run tasks to be run when a message is received.
     *
     * @param fromAddress The address from which the message was received
     * @param fromPort    The port from which the message was received
     * @param message     The message received
     */
    protected void onMessageReceived(String fromAddress, int fromPort, Message message) {
        logger.debug("Message received from " + fromAddress + ": " + message);
        synchronized (listenersListKey) {
            listenersList.forEach(listener -> listener.onMessageReceived(fromAddress, fromPort, message));
        }
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public void registerListener(NetworkHandlerListener listener) {
        synchronized (listenersListKey) {
            if (listenersList.add(listener)) {
                logger.debug("Registered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to register network handler listener " + listener.getClass());
            }
        }
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     */
    public void unregisterListener(NetworkHandlerListener listener) {
        synchronized (listenersListKey) {
            if (listenersList.remove(listener)) {
                logger.debug("Unregistered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to unregister network handler listener " + listener.getClass());
            }
        }
    }

    /**
     * Unregister all existing listener.
     */
    public void clearListeners() {
        synchronized (listenersListKey) {
            listenersList.clear();
        }
        logger.debug("Cleared network handler listeners");
    }
}

