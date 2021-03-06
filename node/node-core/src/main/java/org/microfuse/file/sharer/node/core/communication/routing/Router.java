package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.network.BootstrapServerNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerListener;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.SuperPeerRandomWalkRoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredRandomWalkRoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.tracing.Tracer;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Router class.
 * <p>
 * This governs how the messages are routed through the P2P network.
 */
public class Router implements NetworkHandlerListener {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private ServiceHolder serviceHolder;

    private final ReadWriteLock listenersListLock;
    private final ReadWriteLock routingTableLock;
    private final ReadWriteLock routingStrategyLock;
    private final ReadWriteLock networkHandlerLock;
    private final ReadWriteLock bootstrapServerNetworkHandlerLock;
    private final Lock heartBeatLock;

    private List<RouterListener> listenersList;
    private RoutingTable routingTable;
    private RoutingStrategy routingStrategy;
    private NetworkHandler networkHandler;
    private BootstrapServerNetworkHandler bootstrapServerNetworkHandler;
    private Thread heartBeatThread;
    private boolean heartBeatingEnabled;

    public Router(NetworkHandler networkHandler, RoutingStrategy routingStrategy, ServiceHolder serviceHolder) {
        listenersListLock = new ReentrantReadWriteLock();
        routingTableLock = new ReentrantReadWriteLock();
        routingStrategyLock = new ReentrantReadWriteLock();
        networkHandlerLock = new ReentrantReadWriteLock();
        bootstrapServerNetworkHandlerLock = new ReentrantReadWriteLock();
        heartBeatLock = new ReentrantLock();
        try {
            routingTable = RoutingTable.getRoutingTableClass(serviceHolder.getPeerType())
                    .getConstructor(ServiceHolder.class).newInstance(serviceHolder);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            logger.error("Failed to instantiate routing table for " + serviceHolder.getPeerType().getValue()
                    + ". Using the routing table for " + PeerType.ORDINARY_PEER.getValue() + " instead", e);
            routingTable = new OrdinaryPeerRoutingTable(serviceHolder);
        }
        heartBeatingEnabled = false;
        listenersList = new ArrayList<>();

        bootstrapServerNetworkHandler = new BootstrapServerNetworkHandler(serviceHolder);
        bootstrapServerNetworkHandler.registerListener(this);

        this.serviceHolder = serviceHolder;
        this.routingStrategy = routingStrategy;
        this.networkHandler = networkHandler;
        this.networkHandler.registerListener(this);
        this.networkHandler.startListening();
    }

    @Override
    public void onMessageReceived(String fromAddress, int fromPort, Message message) {
        logger.info("Received message " + message.toString() + " from node " + fromAddress
                + ":" + fromPort);
        MessageType messageType = message.getType();

        // Finding the node object from the routing table
        Node fromNode;
        routingTableLock.readLock().lock();
        try {
            fromNode = routingTable.getUnstructuredNetworkRoutingTableNode(fromAddress, fromPort);
        } finally {
            routingTableLock.readLock().unlock();
        }

        // Creating new node object if it is not present in the routing table
        if (fromNode == null) {
            fromNode = new Node();
            fromNode.setIp(fromAddress);
            fromNode.setPort(fromPort);
            fromNode.setState(NodeState.ACTIVE);
        }

        // Running tasks based on the message type
        if (messageType != null && (messageType == MessageType.SER || messageType == MessageType.SER_SUPER_PEER)) {
            route(fromNode, message);
        } else if (messageType == MessageType.HEARTBEAT) {
            handleHeartBeatMessage(fromNode, message);
        } else if (messageType == MessageType.HEARTBEAT_OK) {
            handleHeartBeatOkMessage(fromNode, message);
        } else {
            runTasksOnMessageReceived(fromNode, message);
        }
    }

    @Override
    public void onMessageSendFailed(String toAddress, int toPort, Message message) {
        logger.info("Sending message " + message.toString() + " to node " + toAddress + ":" + toPort + " failed");

        // Finding the node object from the routing table
        Node receivingNode;
        routingTableLock.readLock().lock();
        try {
            receivingNode = routingTable.getUnstructuredNetworkRoutingTableNode(toAddress, toPort);
            if (receivingNode == null && serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
                receivingNode = superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(toAddress, toPort);
                if (receivingNode == null) {
                    receivingNode = superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(toAddress, toPort);
                }
            }
        } finally {
            routingTableLock.readLock().unlock();
        }

        // Marking the node as inactive
        if (receivingNode != null) {
            receivingNode.setState(NodeState.PENDING_INACTIVATION);
        }

        // Creating new node object if it is not present in the routing table
        if (receivingNode == null) {
            receivingNode = new Node();
            receivingNode.setIp(toAddress);
            receivingNode.setPort(toPort);
        }

        MessageType messageType = message.getType();
        if (messageType != null && (messageType == MessageType.SER || messageType == MessageType.SER_OK ||
                messageType == MessageType.SER_SUPER_PEER || messageType == MessageType.SER_SUPER_PEER_OK ||
                messageType == MessageType.HEARTBEAT || messageType == MessageType.HEARTBEAT_OK)) {
            logger.warn("Message " + message.toString() + " send failed to node " + receivingNode.toString());

            // Trying to retry sending the message based on the message type
            if (routingStrategy instanceof SuperPeerRandomWalkRoutingStrategy ||
                    routingStrategy instanceof UnstructuredRandomWalkRoutingStrategy) {
                boolean retry = false;

                // Updating the hop count
                if (messageType == MessageType.SER) {
                    message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(
                            Integer.parseInt(message.getData(MessageIndexes.SER_HOP_COUNT)) - 1
                    ));
                    retry = true;
                } else if (messageType == MessageType.SER_SUPER_PEER) {
                    message.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT, Integer.toString(
                            Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT)) - 1
                    ));
                    retry = true;
                }

                if (retry) {
                    logger.info("Retrying to send failed message " + message.toString());
                    route(message);
                }
            }
        } else {
            runTasksOnMessageSendFailed(receivingNode, message);
        }
    }

    /**
     * Restart the router with new configuration.
     */
    public void restart() {
        // Clearing the routing table
        routingTableLock.readLock().lock();
        try {
            routingTable.clear();
        } finally {
            routingTableLock.readLock().unlock();
        }

        // Restarting the network handler
        networkHandlerLock.readLock().lock();
        try {
            networkHandler.restart();
        } finally {
            networkHandlerLock.readLock().unlock();
        }
    }

    /**
     * Shutdown the router.
     */
    public void shutdown() {
        // Clearing the routing table
        routingTableLock.readLock().lock();
        try {
            routingTable.clear();
        } finally {
            routingTableLock.readLock().unlock();
        }

        // Shutting down the network handler
        networkHandlerLock.writeLock().lock();
        try {
            networkHandler.shutdown();
        } finally {
            networkHandlerLock.writeLock().unlock();
        }
    }

    /**
     * Enable heart beating.
     */
    public void enableHeartBeat() {
        heartBeatLock.lock();
        try {
            if (!heartBeatingEnabled) {
                heartBeatingEnabled = true;
                heartBeatThread = new Thread(() -> {
                    while (heartBeatingEnabled) {
                        try {
                            Thread.sleep(serviceHolder.getConfiguration().getHeartbeatInterval());
                        } catch (InterruptedException ignored) {
                        }
                        heartBeat();
                    }
                    logger.info("Stopped Heart beating");
                });
                heartBeatThread.setPriority(Thread.MIN_PRIORITY);
                heartBeatThread.setDaemon(true);
                heartBeatThread.start();
                logger.info("Started Heart beating");
            }
        } finally {
            heartBeatLock.unlock();
        }
    }

    /**
     * Disable heart beating.
     */
    public void disableHeartBeat() {
        heartBeatLock.lock();
        try {
            if (heartBeatingEnabled) {
                heartBeatingEnabled = false;
                if (heartBeatThread != null) {
                    heartBeatThread.interrupt();
                }
            }
        } finally {
            heartBeatLock.unlock();
        }
    }

    /**
     * Heartbeat to all nodes and verify if they are alive.
     */
    public void heartBeat() {
        logger.info("Heart beating to check nodes");
        Set<Node> nodes = routingTable.getAll();
        nodes.forEach(node -> {
            // Updating the node state
            if (node.getState() == NodeState.PENDING_INACTIVATION) {
                node.setState(NodeState.INACTIVE);                  // Deleted by the garbage collection thread
            } else if (node.getState() == NodeState.ACTIVE) {
                node.setState(NodeState.PENDING_INACTIVATION);      // Marked inactive in the next heartbeat
            }

            // Preparing the HEARTBEAT message
            Message heartBeatMessage = new Message();
            heartBeatMessage.setType(MessageType.HEARTBEAT);
            heartBeatMessage.setData(MessageIndexes.HEARTBEAT_SOURCE_IP,
                    serviceHolder.getConfiguration().getIp());
            heartBeatMessage.setData(MessageIndexes.HEARTBEAT_SOURCE_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

            logger.info("Heart beat sent to node " + node.toString());
            sendMessage(node, heartBeatMessage);
        });
    }

    /**
     * Send a message directly to a node.
     * Does not wait for reply
     *
     * @param toNode  The node to which the message needs to be sent
     * @param message The message to be sent
     */
    public void sendMessage(Node toNode, Message message) {
        sendMessage(toNode.getIp(), toNode.getPort(), message);
    }

    /**
     * Send a message directly to a node.
     * Does not wait for reply
     *
     * @param ip           The ip of the node to which the message needs to be sent
     * @param port         The port of the node to which the message needs to be sent
     * @param message      The message to be sent
     */
    public void sendMessage(String ip, int port, Message message) {
        networkHandlerLock.readLock().lock();
        try {
            logger.info("Sending message " + message.toString() + " to node " + ip + ":" + port);
            long timeStamp = System.currentTimeMillis();
            networkHandler.sendMessage(ip, port, message);

            // Notifying the tracer
            Tracer tracer = serviceHolder.getTracer();
            if (tracer != null) {
                try {
                    tracer.notifyMessageSend(
                            timeStamp,
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            ip, port, message
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to notify tracer of sending message", e);
                }
            }
        } finally {
            networkHandlerLock.readLock().unlock();
        }
    }

    /**
     * Send a message to the bootstrap server.
     *
     * @param message The message to be sent
     */
    public void sendMessageToBootstrapServer(Message message) {
        String ip = serviceHolder.getConfiguration().getBootstrapServerIP();
        int port = serviceHolder.getConfiguration().getBootstrapServerPort();

        // Sending message to the bootstrap server
        bootstrapServerNetworkHandlerLock.readLock().lock();
        try {
            logger.info("Sending message to bootstrap server " + message.toString() + " to node " + ip + ":" + port);
            long timeStamp = System.currentTimeMillis();
            bootstrapServerNetworkHandler.sendMessage(ip, port, message);

            // Notifying the tracer
            Tracer tracer = serviceHolder.getTracer();
            if (tracer != null) {
                try {
                    tracer.notifyMessageSend(
                            timeStamp,
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            ip, port, message
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to notify tracer of sending message", e);
                }
            }
        } finally {
            bootstrapServerNetworkHandlerLock.readLock().unlock();
        }
    }

    /**
     * Route a message through the P2P network.
     *
     * @param message The message to be sent
     */
    public void route(Message message) {
        logger.info("Routing new message " + message.toString() + " over the network.");
        route(null, message);
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public void promoteToSuperPeer() {
        routingTableLock.writeLock().lock();
        try {
            if (routingTable instanceof OrdinaryPeerRoutingTable) {
                routingTable = new SuperPeerRoutingTable(serviceHolder, (OrdinaryPeerRoutingTable) routingTable);
                logger.info("Changed routing table to super peer routing table.");
            }
        } finally {
            routingTableLock.writeLock().unlock();
        }
        logger.info("Promoted router to super peer router.");
    }

    /**
     * Demote the current node to a super peer.
     */
    public void demoteToOrdinaryPeer() {
        routingTableLock.writeLock().lock();
        try {
            if (routingTable instanceof SuperPeerRoutingTable) {
                routingTable = new OrdinaryPeerRoutingTable(serviceHolder, (SuperPeerRoutingTable) routingTable);
                logger.info("Changed routing table to ordinary peer routing table.");
            }
        } finally {
            routingTableLock.writeLock().unlock();
        }
        logger.info("Demoted router to ordinary peer router.");
    }

    /**
     * Change the network handler used by the router.
     *
     * @param networkHandler The network handler to be used
     */
    public void changeNetworkHandler(NetworkHandler networkHandler) {
        networkHandlerLock.writeLock().lock();
        try {
            this.networkHandler.shutdown();
            this.networkHandler = networkHandler;
            this.networkHandler.startListening();
            logger.info("Network handler changed to " + this.networkHandler.getName());
        } finally {
            networkHandlerLock.writeLock().unlock();
        }
    }

    /**
     * Change the routing strategy used by the router.
     *
     * @param routingStrategy The routing strategy to be used
     */
    public void changeRoutingStrategy(RoutingStrategy routingStrategy) {
        routingStrategyLock.writeLock().lock();
        try {
            this.routingStrategy = routingStrategy;
            logger.info("Routing strategy changed to " + this.routingStrategy.getName());
        } finally {
            routingStrategyLock.writeLock().unlock();
        }
    }

    /**
     * Run tasks to be run when a message intended for this node is received.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message that was received
     */
    public void runTasksOnMessageReceived(Node fromNode, Message message) {
        listenersListLock.readLock().lock();
        try {
            listenersList.stream().parallel()
                    .forEach(routerListener -> routerListener.onMessageReceived(fromNode, message));
            long timeStamp = System.currentTimeMillis();

            // Notifying the tracer
            Tracer tracer = serviceHolder.getTracer();
            if (tracer != null) {
                try {
                    tracer.notifyMessageReceived(
                            timeStamp, fromNode.getIp(), fromNode.getPort(),
                            serviceHolder.getConfiguration().getIp(),
                            serviceHolder.getConfiguration().getPeerListeningPort(),
                            message
                    );
                } catch (RemoteException e) {
                    logger.warn("Failed to notify tracer of sending message", e);
                }
            }
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Runs tasks to be run when an error occurs in sending a message.
     *
     * @param toNode  The node to which the message should be sent
     * @param message The message
     */
    public void runTasksOnMessageSendFailed(Node toNode, Message message) {
        listenersListLock.readLock().lock();
        try {
            listenersList.stream().parallel()
                    .forEach(routerListener -> routerListener.onMessageSendFailed(toNode, message));
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Get the routing table used by this router.
     *
     * @return The routing table
     */
    public RoutingTable getRoutingTable() {
        RoutingTable requestedRoutingTable;
        routingTableLock.readLock().lock();
        try {
            requestedRoutingTable = routingTable;
        } finally {
            routingTableLock.readLock().unlock();
        }
        return requestedRoutingTable;
    }

    /**
     * Get the routing strategy used by this router.
     *
     * @return The routing strategy used by the router
     */
    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    /**
     * Get the network handler used by this router.
     *
     * @return The network handler used by this router
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     * @return True if registering was successful
     */
    public boolean registerListener(RouterListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.add(listener);
            if (isSuccessful) {
                logger.info("Registered router listener " + listener.getClass());
            } else {
                logger.info("Failed to register router listener " + listener.getClass());
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
    public boolean unregisterListener(RouterListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.remove(listener);
            if (isSuccessful) {
                logger.info("Unregistered router listener " + listener.getClass());
            } else {
                logger.info("Failed to unregister router listener " + listener.getClass());
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
            logger.info("Cleared network handler listeners");
        } finally {
            listenersListLock.writeLock().lock();
        }
    }

    /**
     * Route a message through the P2P network.
     *
     * @param fromNode The node from which the message was received by this node
     * @param message  The message to be sent
     */
    private void route(Node fromNode, Message message) {
        MessageType messageType = message.getType();

        if (messageType != null && messageType == MessageType.SER) {
            // Checking owned resources
            Set<OwnedResource> ownedResources = serviceHolder.getResourceIndex()
                    .findOwnedResources(message.getData(MessageIndexes.SER_QUERY));

            if (ownedResources.size() > 0) {
                logger.info("Resource requested by \"" + message.toString() + "\" found in owned resources");

                // Preparing SER_OK message
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_OK);

                // Preparing the SER_OK message data
                List<String> serOkData = new ArrayList<>();
                serOkData.add(MessageIndexes.SER_OK_QUERY_STRING, message.getData(MessageIndexes.SER_QUERY));
                serOkData.add(MessageIndexes.SER_OK_SEQUENCE_NUMBER,
                        message.getData(MessageIndexes.SER_SEQUENCE_NUMBER));
                serOkData.add(MessageIndexes.SER_OK_HOP_COUNT, message.getData(MessageIndexes.SER_HOP_COUNT));
                serOkData.add(MessageIndexes.SER_OK_FILE_COUNT, Integer.toString(ownedResources.size()));
                serOkData.add(MessageIndexes.SER_OK_IP, serviceHolder.getConfiguration().getIp());
                serOkData.add(MessageIndexes.SER_OK_PORT,
                        Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

                // Adding the list of resources to the SER_OK message
                ownedResources.forEach(resource -> serOkData.add(resource.getName()));
                serOkMessage.setData(serOkData);

                logger.info("Sending search request success message \"" + message.toString() + "\" back to "
                        + message.getData(MessageIndexes.SER_SOURCE_IP) + ":"
                        + message.getData(MessageIndexes.SER_SOURCE_PORT));
                sendMessage(message.getData(MessageIndexes.SER_SOURCE_IP),
                        Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
            } else {
                logger.info("Resource requested by \"" + message.toString() + "\" not found in owned resources");

                // Updating the hop count
                Integer hopCount = Integer.parseInt(message.getData(MessageIndexes.SER_HOP_COUNT)) + 1;
                message.setData(MessageIndexes.SER_HOP_COUNT, hopCount.toString());
                logger.info("Increased hop count of message" + message.toString());

                if (hopCount <= serviceHolder.getConfiguration().getTimeToLive()) {
                    forwardNode(fromNode, message);
                } else {
                    logger.info("Dropped message " + message.toString() + " since the hop count of the message "
                            + message.toString() + " is higher than time to live "
                            + serviceHolder.getConfiguration().getTimeToLive());
                }
            }
        } else if (messageType != null && messageType == MessageType.SER_SUPER_PEER) {
            if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
                // Preparing the SER_SUPER_PEER_OK message
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_SUPER_PEER_OK);
                serOkMessage.setData(MessageIndexes.SER_SUPER_PEER_OK_IP, serviceHolder.getConfiguration().getIp());
                serOkMessage.setData(MessageIndexes.SER_SUPER_PEER_OK_PORT,
                        Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                serOkMessage.setData(MessageIndexes.SER_SUPER_PEER_OK_SEQUENCE_NUMBER,
                        message.getData(MessageIndexes.SER_SUPER_PEER_SEQUENCE_NUMBER));
                serOkMessage.setData(MessageIndexes.SER_SUPER_PEER_OK_HOP_COUNT,
                        message.getData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT));

                sendMessage(message.getData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP),
                        Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT)),
                        serOkMessage);
            } else if (serviceHolder.getPeerType() == PeerType.ORDINARY_PEER) {
                logger.info("Node is an ordinary peer");

                // Updating the hop count
                Integer hopCount = Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT)) + 1;
                message.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT, hopCount.toString());
                logger.info("Increased hop count of message" + message.toString());

                if (hopCount <= serviceHolder.getConfiguration().getTimeToLive()) {
                    forwardNode(fromNode, message);
                } else {
                    logger.info("Dropping message " + message.toString() + " since the hop count of the message "
                            + message.toString() + " is higher than time to live "
                            + serviceHolder.getConfiguration().getTimeToLive());
                }
            }
        } else {
            logger.warn("Not routing message of type " + messageType + ". The Router will only route messages of type "
                    + MessageType.SER + ".");
        }
    }

    /**
     * Forward messages to nodes based on the routing strategy.
     *
     * @param fromNode The node from which the message was received by this node
     * @param message  The message to be sent
     */
    private void forwardNode(Node fromNode, Message message) {
        logger.info("The hop count of the message " + message.toString() + " is lower than time to live "
                + serviceHolder.getConfiguration().getTimeToLive());

        // Using the routing strategy to get the nodes the message should be routed to
        Set<Node> forwardingNodes;
        routingTableLock.readLock().lock();
        try {
            routingStrategyLock.readLock().lock();
            try {
                forwardingNodes = routingStrategy.getForwardingNodes(routingTable, fromNode, message);
            } finally {
                routingStrategyLock.readLock().unlock();
            }
        } finally {
            routingTableLock.readLock().unlock();
        }

        // Sending to all the selected nodes
        forwardingNodes.stream().parallel()
                .forEach(forwardingNode -> {
                    Message clonedMessage = message.clone();
                    logger.info("Forwarding message " + clonedMessage.toString()
                            + " to " + forwardingNode.toString());
                    sendMessage(forwardingNode.getIp(), forwardingNode.getPort(), clonedMessage);
                });
    }

    /**
     * Handle HEARTBEAT type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleHeartBeatMessage(Node fromNode, Message message) {
        // Preparing the HEARTBEAT_OK message
        Message heartBeatOkMessage = new Message();
        heartBeatOkMessage.setType(MessageType.HEARTBEAT_OK);
        heartBeatOkMessage.setData(MessageIndexes.HEARTBEAT_OK_IP, serviceHolder.getConfiguration().getIp());
        heartBeatOkMessage.setData(MessageIndexes.HEARTBEAT_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

        // Fetching the node which sent the HEARTBEAT message
        Node node = routingTable.get(
                message.getData(MessageIndexes.HEARTBEAT_SOURCE_IP),
                Integer.parseInt(message.getData(MessageIndexes.HEARTBEAT_SOURCE_PORT))
        );

        // Marking the node as ACTIVE
        if (node != null) {
            node.setState(NodeState.ACTIVE);
        }

        logger.info("Sending heartbeat ok message back to "
                + message.getData(MessageIndexes.HEARTBEAT_SOURCE_IP) + ":"
                + message.getData(MessageIndexes.HEARTBEAT_SOURCE_PORT));
        sendMessage(
                message.getData(MessageIndexes.HEARTBEAT_SOURCE_IP),
                Integer.parseInt(message.getData(MessageIndexes.HEARTBEAT_SOURCE_PORT)),
                heartBeatOkMessage
        );
    }

    /**
     * Handle HEARTBEAT_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleHeartBeatOkMessage(Node fromNode, Message message) {
        // Fetching the node which sent the HEARTBEAT_OK message
        Node node = routingTable.get(
                message.getData(MessageIndexes.HEARTBEAT_OK_IP),
                Integer.parseInt(message.getData(MessageIndexes.HEARTBEAT_OK_PORT))
        );

        // Marking the node as active
        if (node != null) {
            node.setState(NodeState.ACTIVE);
        }
    }
}
