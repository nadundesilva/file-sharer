package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.network.rmi.RMINetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.udp.UDPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    protected ServiceHolder serviceHolder;

    private static Map<NetworkHandlerType, Class<? extends NetworkHandler>> networkHandlerClassMap;

    private final ReadWriteLock listenersListLock;
    private final ReadWriteLock listenerHandlerExecutorServiceLock;
    private List<NetworkHandlerListener> listenersList;
    private ExecutorService listenerHandlerExecutorService;

    protected boolean restartRequired;
    protected boolean running;

    static {
        // Populating the network handler class map
        networkHandlerClassMap = new HashMap<>();
        networkHandlerClassMap.put(NetworkHandlerType.WEB_SERVICES, WebServicesNetworkHandler.class);
        networkHandlerClassMap.put(NetworkHandlerType.RMI, RMINetworkHandler.class);
        networkHandlerClassMap.put(NetworkHandlerType.TCP_SOCKET, TCPSocketNetworkHandler.class);
        networkHandlerClassMap.put(NetworkHandlerType.UDP_SOCKET, UDPSocketNetworkHandler.class);
    }

    public NetworkHandler(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;
        listenersListLock = new ReentrantReadWriteLock();
        listenerHandlerExecutorServiceLock = new ReentrantReadWriteLock();
        listenersList = new ArrayList<>();
        listenerHandlerExecutorService =
                Executors.newFixedThreadPool(this.serviceHolder.getConfiguration().getNetworkHandlerThreadCount());
        restartRequired = false;
        running = false;
    }

    /**
     * Get the network handler class based on the network handler type.
     *
     * @param networkHandlerType The network handler type
     * @return The network handler class
     */
    public static Class<? extends NetworkHandler> getNetworkHandlerClass(NetworkHandlerType networkHandlerType) {
        return networkHandlerClassMap.get(networkHandlerType);
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
    public void startListening() {
        running = true;
    }

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
        try {
            listenerHandlerExecutorService.shutdown();
            listenerHandlerExecutorService =
                    Executors.newFixedThreadPool(serviceHolder.getConfiguration().getNetworkHandlerThreadCount());
        } finally {
            listenerHandlerExecutorServiceLock.writeLock().unlock();
            restartRequired = false;
        }
    }

    /**
     * Close the network handler.
     */
    public abstract void shutdown();

    /**
     * Run tasks to be run when a message is received.
     *
     * @param fromAddress The address from which the message was received
     * @param fromPort    The port from which the message was received
     * @param message     The message received
     */
    protected void runTasksOnMessageReceived(String fromAddress, int fromPort, Message message) {
        logger.debug("Message " + message.toString() + " received from node " + fromAddress + ":" + fromPort);
        listenersListLock.readLock().lock();
        try {
            listenerHandlerExecutorServiceLock.readLock().lock();
            try {
                listenersList.forEach(listener -> listenerHandlerExecutorService.execute(() ->
                        listener.onMessageReceived(fromAddress, fromPort, message)
                ));
            } finally {
                listenerHandlerExecutorServiceLock.readLock().unlock();
            }
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Runs tasks to be run when an error occurs in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param toPort    The port to which the message should be sent
     * @param message   The message
     */
    protected void runTasksOnMessageSendFailed(String toAddress, int toPort, Message message) {
        logger.debug("Failed to send message " + message + " to " + toAddress + ":" + toPort);
        listenersListLock.readLock().lock();
        try {
            listenerHandlerExecutorServiceLock.readLock().lock();
            try {
                listenersList.forEach(listener -> listenerHandlerExecutorService.execute(() ->
                        listener.onMessageSendFailed(toAddress, toPort, message)
                ));
            } finally {
                listenerHandlerExecutorServiceLock.readLock().unlock();
            }
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     * @return True if registering listener was successful
     */
    public boolean registerListener(NetworkHandlerListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.add(listener);
            if (isSuccessful) {
                logger.debug("Registered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to register network handler listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     * @return True if unregister was successful
     */
    public boolean unregisterListener(NetworkHandlerListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.remove(listener);
            if (isSuccessful) {
                logger.debug("Unregistered network handler listener " + listener.getClass());
            } else {
                logger.debug("Failed to unregister network handler listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
        return isSuccessful;
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

