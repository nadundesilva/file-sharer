package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The Network Handler SuperClass.
 * <p>
 * All types of network handlers should extend this abstract class.
 */
public abstract class NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

    private List<NetworkHandlerListener> listenersList;
    private ExecutorService listenerHandlerExecutorService;
    protected boolean restartRequired;

    private final ReadWriteLock listenersListLock;
    private final ReadWriteLock listenerHandlerExecutorServiceLock;

    public NetworkHandler() {
        listenersListLock = new ReentrantReadWriteLock();
        listenerHandlerExecutorServiceLock = new ReentrantReadWriteLock();
        listenersList = new ArrayList<>();
        listenerHandlerExecutorService =
                Executors.newFixedThreadPool(ServiceHolder.getConfiguration().getListenerHandlingThreadCount());
        restartRequired = false;
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
     * Restart the listening.
     */
    public void restart() {
        restartRequired = true;
        listenerHandlerExecutorServiceLock.writeLock().lock();
        listenerHandlerExecutorService.shutdown();
        listenerHandlerExecutorService =
                Executors.newFixedThreadPool(ServiceHolder.getConfiguration().getListenerHandlingThreadCount());
        logger.debug("Setting the restart required flag");
        listenerHandlerExecutorServiceLock.writeLock().unlock();
    }

    /**
     * Runs tasks to be run when an error occurs in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param toPort    The port to which the message should be sent
     * @param message   The message
     */
    protected void onMessageSendFailed(String toAddress, int toPort, Message message) {
        logger.debug("Failed to send message to " + toAddress + ": " + message);
        listenersListLock.readLock().lock();
        try {
            listenerHandlerExecutorServiceLock.readLock().lock();
            listenersList.forEach(listener -> listenerHandlerExecutorService.execute(() ->
                    listener.onMessageSendFailed(toAddress, toPort, message)
            ));
            listenerHandlerExecutorServiceLock.readLock().unlock();
        } finally {
            listenersListLock.readLock().unlock();
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
        listenersListLock.readLock().lock();
        try {
            listenerHandlerExecutorServiceLock.readLock().lock();
            listenersList.forEach(listener -> listenerHandlerExecutorService.execute(() ->
                    listener.onMessageReceived(fromAddress, fromPort, message)
            ));
            listenerHandlerExecutorServiceLock.readLock().unlock();
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public boolean registerListener(NetworkHandlerListener listener) {
        boolean isSuccessul;
        listenersListLock.writeLock().lock();
        try {
            isSuccessul = listenersList.add(listener);
            if (isSuccessul) {
                logger.debug("Registered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to register network handler listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
        return isSuccessul;
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     */
    public void unregisterListener(NetworkHandlerListener listener) {
        listenersListLock.writeLock().lock();
        try {
            if (listenersList.remove(listener)) {
                logger.debug("Unregistered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to unregister network handler listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
    }

    /**
     * Unregister all existing listener.
     */
    public void clearListeners() {
        listenersListLock.writeLock().lock();
        try {
            listenersList.clear();
            logger.debug("Cleared network handler listeners");
        } finally {
            listenersListLock.writeLock().unlock();
        }
    }
}

