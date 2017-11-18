package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageConstants;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Overlay Network Manager.
 */
public class OverlayNetworkManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(OverlayNetworkManager.class);

    private ServiceHolder serviceHolder;

    private final Lock gossipingLock;
    private Thread gossipingThread;
    private boolean gossipingEnabled;
    private Thread serSuperPeerStartThread;
    private Thread serSuperPeerTimeoutThread;

    private final Lock fullSuperPeerCacheLock;
    private Set<Node> fullSuperPeerCache;
    private long sequenceNumber;

    public OverlayNetworkManager(ServiceHolder serviceHolder) {
        gossipingLock = new ReentrantLock();
        gossipingEnabled = false;

        fullSuperPeerCacheLock = new ReentrantLock();
        fullSuperPeerCache = new HashSet<>();
        sequenceNumber = 0;

        this.serviceHolder = serviceHolder;
        this.serviceHolder.getRouter().registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        logger.info("Received message " + message.toString() + " from node " + fromNode.toString());
        switch (message.getType()) {
            case REG_OK:
                handleRegOkMessage(fromNode, message);
                break;
            case UNREG_OK:
                handleUnregOkMessage(fromNode, message);
                break;
            case JOIN:
                handleJoinMessage(fromNode, message);
                break;
            case JOIN_OK:
                handleJoinOkMessage(fromNode, message);
                break;
            case LEAVE:
                handleLeaveMessage(fromNode, message);
                break;
            case LEAVE_OK:
                handleLeaveOkMessage(fromNode, message);
                break;
            case SER_SUPER_PEER_OK:
                handleSerSuperPeerOkMessage(fromNode, message);
                break;
            case JOIN_SUPER_PEER:
                handleJoinSuperPeerMessage(fromNode, message);
                break;
            case JOIN_SUPER_PEER_OK:
                handleJoinSuperPeerOkMessage(fromNode, message);
                break;
            case LIST_RESOURCES:
                handleListResourcesMessage(fromNode, message);
                break;
            case LIST_RESOURCES_OK:
                handleListResourcesOkMessage(fromNode, message);
                break;
            case LIST_UNSTRUCTURED_CONNECTIONS:
                handleListUnstructuredConnectionsMessage(fromNode, message);
                break;
            case LIST_UNSTRUCTURED_CONNECTIONS_OK:
                handleListUnstructuredConnectionsOkMessage(fromNode, message);
                break;
            case LIST_SUPER_PEER_CONNECTIONS:
                handleListSuperPeerConnectionsMessage(fromNode, message);
                break;
            case LIST_SUPER_PEER_CONNECTIONS_OK:
                handleListSuperPeerConnectionsOkMessage(fromNode, message);
                break;
            default:
                logger.info("Ignored message " + message.toString() + " of unrecognized type");
        }
    }

    @Override
    public void onMessageSendFailed(Node toNode, Message message) {
        logger.info("Sending message " + message.toString() + " failed to node " + toNode.toString());
        switch (message.getType()) {
            case JOIN_SUPER_PEER:
                searchForSuperPeer();
                break;
            default:
                logger.info("Ignored message send failed " + message.toString() + " of unrecognized type");
        }
    }

    /**
     * Enable gossiping.
     */
    public void enableGossiping() {
        gossipingLock.lock();
        try {
            if (!gossipingEnabled) {
                gossipingEnabled = true;
                gossipingThread = new Thread(() -> {
                    while (gossipingEnabled) {
                        try {
                            Thread.sleep(serviceHolder.getConfiguration().getGossipingInterval());
                        } catch (InterruptedException ignored) {
                        }
                        gossip();
                    }
                    logger.info("Stopped gossiping");
                });
                gossipingThread.setPriority(Thread.MIN_PRIORITY);
                gossipingThread.setDaemon(true);
                gossipingThread.start();
                logger.info("Started gossiping");
            }
        } finally {
            gossipingLock.unlock();
        }
    }

    /**
     * Disable gossiping.
     */
    public void disableGossiping() {
        gossipingLock.lock();
        try {
            if (gossipingEnabled) {
                gossipingEnabled = false;
                if (gossipingThread != null) {
                    gossipingThread.interrupt();
                }
            }
        } finally {
            gossipingLock.unlock();
        }
    }

    /**
     * Gossip and grow the network.
     * This will only happen if the unstructured network peer count is less than the threshold.
     */
    public void gossip() {
        RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();

        if (routingTable.getAll().size() > 0) {
            // Finding unstructured connections
            List<Node> unstructuredNodes = routingTable.getAllUnstructuredNetworkNodes().stream().parallel()
                    .filter(Node::isActive)
                    .collect(Collectors.toList());
            if (unstructuredNodes.size() < serviceHolder.getConfiguration().getMaxUnstructuredPeerCount()) {
                logger.info("Gossiping to grow the unstructured network");
                int selectedIndex = ThreadLocalRandom.current().nextInt(0, unstructuredNodes.size());
                requestUnstructuredConnections(new HashSet<>(Collections.singletonList(
                        unstructuredNodes.get(selectedIndex)
                )));
            }

            // Finding super peers
            if (routingTable instanceof SuperPeerRoutingTable) {
                List<Node> superPeerNodes = ((SuperPeerRoutingTable) routingTable).getAllSuperPeerNetworkNodes()
                        .stream().parallel()
                        .filter(Node::isActive)
                        .collect(Collectors.toList());
                if (superPeerNodes.size() > 0) {
                    if (superPeerNodes.size() < serviceHolder.getConfiguration().getMaxSuperPeerCount()) {
                        logger.info("Gossiping to grow the super peer network");
                        int selectedSuperPeerIndex =
                                ThreadLocalRandom.current().nextInt(0, superPeerNodes.size());
                        requestSuperPeerConnections(new HashSet<>(Collections.singletonList(
                                superPeerNodes.get(selectedSuperPeerIndex)
                        )));
                    }
                } else {
                    searchForSuperPeer();
                }
            } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
                if (ordinaryPeerRoutingTable.getAssignedSuperPeer() == null) {
                    searchForSuperPeer();
                }
            }
        } else {
            // Isolated node
            unregister();
            try {
                Thread.sleep(Constants.TASK_INTERVAL);
            } catch (InterruptedException ignored) {
            }
            register();
        }

        if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
            logger.info("Gossiping to grow the aggregated resources index");

            if (routingTable instanceof SuperPeerRoutingTable) {
                requestOwnedResourcesList((SuperPeerRoutingTable) routingTable);
            } else {
                logger.error("Inconsistent ordinary peer routing table in super peer");
            }
        }
    }

    /**
     * Register the current node in the bootstrap server.
     */
    public void register() {
        Message regMessage = new Message();
        regMessage.setType(MessageType.REG);
        regMessage.setData(MessageIndexes.REG_IP, serviceHolder.getConfiguration().getIp());
        regMessage.setData(MessageIndexes.REG_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
        regMessage.setData(MessageIndexes.REG_USERNAME,
                serviceHolder.getConfiguration().getUsernamePrefix()
                        + serviceHolder.getConfiguration().getIp() + ":"
                        + serviceHolder.getConfiguration().getPeerListeningPort());
        serviceHolder.getRouter().sendMessageToBootstrapServer(regMessage);
    }

    /**
     * Unregister the current node from the bootstrap server.
     */
    public void unregister() {
        Message unregMessage = new Message();
        unregMessage.setType(MessageType.UNREG);
        unregMessage.setData(MessageIndexes.UNREG_IP, serviceHolder.getConfiguration().getIp());
        unregMessage.setData(MessageIndexes.UNREG_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
        unregMessage.setData(MessageIndexes.UNREG_USERNAME, serviceHolder.getConfiguration().getUsernamePrefix());
        serviceHolder.getRouter().sendMessageToBootstrapServer(unregMessage);
    }

    /**
     * Join the system.
     *
     * @param nodes The nodes to connect to
     */
    private void join(Set<Node> nodes) {
        nodes.forEach(node -> {
            Message joinMessage = new Message();
            joinMessage.setType(MessageType.JOIN);
            joinMessage.setData(MessageIndexes.JOIN_IP, serviceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
            serviceHolder.getRouter().sendMessage(node, joinMessage);
        });
    }

    /**
     * Leave the system.
     */
    public void leave() {
        serviceHolder.getRouter().getRoutingTable().getAll().forEach(node -> {
            Message leaveMessage = new Message();
            leaveMessage.setType(MessageType.LEAVE);
            leaveMessage.setData(MessageIndexes.LEAVE_IP, serviceHolder.getConfiguration().getIp());
            leaveMessage.setData(MessageIndexes.LEAVE_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
            serviceHolder.getRouter().sendMessage(node, leaveMessage);
        });
    }

    /**
     * Heartbeat to all nodes.
     */
    public void heartBeat() {
        serviceHolder.getRouter().enableHeartBeat();
    }

    /**
     * Enable heart beating.
     */
    public void enableHeartBeat() {
        serviceHolder.getRouter().enableHeartBeat();
    }

    /**
     * Enable heart beating.
     */
    public void disableHeartBeat() {
        serviceHolder.getRouter().disableHeartBeat();
    }

    /**
     * Search for a super peer in the system.
     */
    public void searchForSuperPeer() {
        if (serSuperPeerStartThread == null) {
            serSuperPeerStartThread = new Thread(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(
                            serviceHolder.getConfiguration().getSerSuperPeerTimeout(),
                            serviceHolder.getConfiguration().getSerSuperPeerTimeout() * 2
                    ));
                } catch (InterruptedException ignored) {
                }
                logger.info("Searching for super peer");
                if (serSuperPeerStartThread != null) {
                    RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
                    if (routingTable instanceof OrdinaryPeerRoutingTable) {
                        OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
                        if (ordinaryPeerRoutingTable.getAssignedSuperPeer() == null) {
                            Message searchSuperPeerMessage = new Message();
                            searchSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);
                            searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SEQUENCE_NUMBER,
                                    Long.toString(sequenceNumber++));
                            searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP,
                                    serviceHolder.getConfiguration().getIp());
                            searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT,
                                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                            searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                                    Integer.toString(NodeConstants.INITIAL_HOP_COUNT));
                            serviceHolder.getRouter().route(searchSuperPeerMessage);

                            serSuperPeerTimeoutThread = new Thread(() -> {
                                try {
                                    Thread.sleep(serviceHolder.getConfiguration().getSerSuperPeerTimeout());
                                } catch (InterruptedException ignored) {
                                }
                                logger.info("Search for super peer timed out");
                                if (serSuperPeerTimeoutThread != null) {
                                    selfPromoteSuperPeer(false);
                                    serSuperPeerTimeoutThread = null;
                                }
                            });
                            serSuperPeerTimeoutThread.start();
                        }
                    }
                    serSuperPeerStartThread = null;
                }
            });
            serSuperPeerStartThread.setDaemon(true);
            serSuperPeerStartThread.start();
        }
    }

    /**
     * Cancel the searching for super peers.
     */
    public void cancelSearchForSuperPeer() {
        logger.info("Cancelling search for super peer");
        if (serSuperPeerStartThread != null) {
            serSuperPeerStartThread.interrupt();
            Thread thread = serSuperPeerStartThread;
            serSuperPeerStartThread = null;
            thread.interrupt();
        }

        if (serSuperPeerTimeoutThread != null) {
            Thread thread = serSuperPeerTimeoutThread;
            serSuperPeerTimeoutThread = null;
            thread.interrupt();
        }
    }

    /**
     * Self assign current node as super peer.
     *
     * @param joinSuperPeers Join the super peers in the super peer cache.
     */
    private void selfPromoteSuperPeer(boolean joinSuperPeers) {
        logger.info("Self promote super peer");
        serviceHolder.promoteToSuperPeer();

        if (joinSuperPeers) {
            logger.info("Connecting to all the nodes in the super peer cache");
            fullSuperPeerCacheLock.lock();
            try {
                for (Node node : fullSuperPeerCache) {
                    connectToSuperPeer(node.getIp(), node.getPort());
                }
            } finally {
                fullSuperPeerCacheLock.unlock();
            }
        }
        // TODO : Implement self assigning super peer. (Announce to others ?)
    }

    /**
     * Connect to a super peer.
     */
    private void connectToSuperPeer(String ip, int port) {
        PeerType currentPeerType = serviceHolder.getPeerType();

        Message joinMessage = new Message();
        joinMessage.setType(MessageType.JOIN_SUPER_PEER);
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_TYPE, currentPeerType.toString());
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP, serviceHolder.getConfiguration().getIp());
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
        if (currentPeerType == PeerType.ORDINARY_PEER) {
            List<OwnedResource> ownedResources =
                    new ArrayList<>(serviceHolder.getResourceIndex().getAllOwnedResources());
            joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_RESOURCE_COUNT, Integer.toString(ownedResources.size()));
            for (int i = 0; i < ownedResources.size(); i++) {
                joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_RESOURCE_START_INDEX + i,
                        ownedResources.get(i).getName());
            }
        }
        serviceHolder.getRouter().sendMessage(ip, port, joinMessage);
    }

    /**
     * Handle REG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleRegOkMessage(Node fromNode, Message message) {
        switch (message.getData(MessageIndexes.REG_OK_NODES_COUNT)) {
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR:
                logger.error("Unknown error in registering with bootstrap server "
                        + serviceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + serviceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_REGISTERED:
                logger.warn("Current node already registered to bootstrap server "
                        + serviceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + serviceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_OCCUPIED:
                logger.warn("Already registered to bootstrap server "
                        + serviceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + serviceHolder.getConfiguration().getPeerListeningPort());

                // Retrying
                serviceHolder.getConfiguration()
                        .setPeerListeningPort(serviceHolder.getConfiguration().getPeerListeningPort() + 1);
                logger.info("Changing peer listening port to "
                        + serviceHolder.getConfiguration().getPeerListeningPort() + " and retrying");
                register();
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_FULL:
                logger.warn("Bootstrap server "
                        + serviceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + serviceHolder.getConfiguration().getPeerListeningPort() + " full");
                break;
            default:
                int nodesCount = Integer.parseInt(message.getData(MessageIndexes.REG_OK_NODES_COUNT));
                Set<Node> nodesSet = new HashSet<>();
                if (nodesCount > 0) {
                    if (nodesCount <= 2) {
                        for (int i = 0; i < nodesCount * 2; i += 2) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START + i;
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex + 1));
                            nodesSet.add(node);
                        }
                    } else {
                        for (int i = 0; i < 2; ) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                                    + ThreadLocalRandom.current().nextInt(0, nodesCount);
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex + 1));

                            if (!nodesSet.contains(node)) {
                                nodesSet.add(node);
                                i++;
                            }
                        }
                    }


                }
                if (nodesSet.size() > 0) {
                    join(nodesSet);
                } else {
                    selfPromoteSuperPeer(false);
                }
        }
    }

    /**
     * Handle UNREG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleUnregOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE), MessageConstants.UNREG_OK_VALUE_SUCCESS)) {
            logger.info("Successfully unregistered from bootstrap server");
        } else if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE),
                MessageConstants.UNREG_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.info("Unknown value " + message.getData(MessageIndexes.UNREG_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle JOIN type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinMessage(Node fromNode, Message message) {
        String ip = message.getData(MessageIndexes.JOIN_IP);
        int port = Integer.parseInt(message.getData(MessageIndexes.JOIN_PORT));

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.JOIN_OK);

        String joinOkValue;
        int unstructuredNetworkConnectionsCount =
                serviceHolder.getRouter().getRoutingTable().getAllUnstructuredNetworkNodes().stream().parallel()
                .filter(Node::isActive)
                .collect(Collectors.toList()).size();
        if (unstructuredNetworkConnectionsCount < serviceHolder.getConfiguration().getMaxUnstructuredPeerCount()) {
            if (serviceHolder.getRouter().getRoutingTable().addUnstructuredNetworkRoutingTableEntry(ip, port)) {
                joinOkValue = MessageConstants.JOIN_OK_VALUE_SUCCESS;
            } else {
                joinOkValue = MessageConstants.JOIN_OK_VALUE_ERROR;
            }
        } else {
            joinOkValue = MessageConstants.JOIN_OK_VALUE_ERROR_FULL;
        }
        replyMessage.setData(MessageIndexes.JOIN_OK_VALUE, joinOkValue);

        if (Objects.equals(joinOkValue, MessageConstants.JOIN_OK_VALUE_SUCCESS)) {
            String superPeerNodeIP = null;
            int superPeerNodePort = -1;

            RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
            if (routingTable instanceof SuperPeerRoutingTable) {
                superPeerNodeIP = serviceHolder.getConfiguration().getIp();
                superPeerNodePort = serviceHolder.getConfiguration().getPeerListeningPort();
            } else if (routingTable instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                if (ordinaryPeerRoutingTable.getAssignedSuperPeer() != null &&
                        ordinaryPeerRoutingTable.getAssignedSuperPeer().isActive()) {
                    superPeerNodeIP = ordinaryPeerRoutingTable.getAssignedSuperPeer().getIp();
                    superPeerNodePort = ordinaryPeerRoutingTable.getAssignedSuperPeer().getPort();
                } else {
                    logger.info("Assigned super peer is dead");
                }
            } else {
                logger.warn("Unknown routing table type");
            }

            replyMessage.setData(MessageIndexes.JOIN_OK_IP, serviceHolder.getConfiguration().getIp());
            replyMessage.setData(MessageIndexes.JOIN_OK_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
            if (superPeerNodeIP != null && superPeerNodePort >= 1) {
                replyMessage.setData(MessageIndexes.JOIN_OK_SUPER_PEER_IP, superPeerNodeIP);
                replyMessage.setData(MessageIndexes.JOIN_OK_SUPER_PEER_PORT, Integer.toString(superPeerNodePort));
            }
        } else if (Objects.equals(joinOkValue, MessageConstants.JOIN_OK_VALUE_ERROR_FULL)) {
            List<Node> unstructuredNodes = serviceHolder.getRouter().getRoutingTable().getAllUnstructuredNetworkNodes()
                    .stream().parallel()
                    .filter(Node::isActive)
                    .collect(Collectors.toList());
            int selectedIndex = ThreadLocalRandom.current().nextInt(0, unstructuredNodes.size());
            Node selectedNode = unstructuredNodes.get(selectedIndex);

            replyMessage.setData(MessageIndexes.JOIN_OK_NEW_IP, selectedNode.getIp());
            replyMessage.setData(MessageIndexes.JOIN_OK_NEW_PORT, Integer.toString(selectedNode.getPort()));
        }
        serviceHolder.getRouter().sendMessage(ip, port, replyMessage);
    }

    /**
     * Handle JOIN_OK type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinOkMessage(Node fromNode, Message message) {
        String newIP = message.getData(MessageIndexes.JOIN_OK_IP);
        int newPort = Integer.parseInt(message.getData(MessageIndexes.JOIN_OK_PORT));

        if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE), MessageConstants.JOIN_OK_VALUE_SUCCESS)) {

            RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
            routingTable.addUnstructuredNetworkRoutingTableEntry(newIP, newPort);

            if (routingTable instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                if (ordinaryPeerRoutingTable.getAssignedSuperPeer() == null) {

                    if (MessageIndexes.JOIN_OK_SUPER_PEER_IP < message.getData().size() &&
                            MessageIndexes.JOIN_OK_SUPER_PEER_PORT < message.getData().size()) {
                        String superPeerIP = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_IP);
                        String superPeerPort = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_PORT);

                        if (serviceHolder.getRouter().getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                            Node superPeerNode = new Node();
                            superPeerNode.setIp(superPeerIP);
                            superPeerNode.setPort(superPeerPort);
                            routingTable.addUnstructuredNetworkRoutingTableEntry(newIP, newPort);

                            connectToSuperPeer(superPeerIP, Integer.parseInt(superPeerPort));
                        } else {
                            logger.warn("Inconsistent super peer routing table in new node");
                        }
                    } else {
                        searchForSuperPeer();
                    }
                }
            }
        } else if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE),
                MessageConstants.JOIN_OK_VALUE_ERROR_FULL)) {
            Node node = new Node(
                    message.getData(MessageIndexes.JOIN_OK_NEW_IP),
                    Integer.parseInt(message.getData(MessageIndexes.JOIN_OK_NEW_PORT))
            );
            join(new HashSet<>(Collections.singletonList(node)));
        } else if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE),
                MessageConstants.JOIN_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + newIP + ":" + newPort);
        } else {
            logger.info("Unknown value " + message.getData(MessageIndexes.JOIN_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle LEAVE type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveMessage(Node fromNode, Message message) {
        String ip = message.getData(MessageIndexes.LEAVE_IP);
        int port = Integer.parseInt(message.getData(MessageIndexes.LEAVE_PORT));

        boolean isSuccessful = serviceHolder.getRouter().getRoutingTable().removeFromAll(ip, port);

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LEAVE_OK);
        replyMessage.setData(
                MessageIndexes.LEAVE_OK_VALUE,
                (isSuccessful ? MessageConstants.LEAVE_OK_VALUE_SUCCESS : MessageConstants.LEAVE_OK_VALUE_ERROR)
        );
        replyMessage.setData(MessageIndexes.LEAVE_OK_IP, serviceHolder.getConfiguration().getIp());
        replyMessage.setData(
                MessageIndexes.LEAVE_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort())
        );
        serviceHolder.getRouter().sendMessage(ip, port, replyMessage);
    }

    /**
     * Handle LEAVE_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveOkMessage(Node fromNode, Message message) {
        String leavingIP = message.getData(MessageIndexes.LEAVE_OK_IP);
        int leavingPort = Integer.parseInt(message.getData(MessageIndexes.LEAVE_OK_PORT));

        if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE), MessageConstants.LEAVE_OK_VALUE_SUCCESS)) {
            serviceHolder.getRouter().getRoutingTable().removeFromAll(leavingIP, leavingPort);

            ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
            if (resourceIndex instanceof SuperPeerResourceIndex) {
                ((SuperPeerResourceIndex) resourceIndex).removeNodeFromAggregatedResources(leavingIP, leavingPort);
            }
        } else if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE),
                MessageConstants.LEAVE_OK_VALUE_ERROR)) {
            logger.warn("Failed to disconnect unstructured connection with " + leavingIP + ":" + leavingPort);
        } else {
            logger.info("Unknown value " + message.getData(MessageIndexes.LEAVE_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle SER_SUPER_PEER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleSerSuperPeerOkMessage(Node fromNode, Message message) {
        if (serSuperPeerStartThread != null) {
            cancelSearchForSuperPeer();
            connectToSuperPeer(message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                    Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT)));
        }
    }

    /**
     * Handle JOIN_SUPER_PEER type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinSuperPeerMessage(Node fromNode, Message message) {
        Message replyMessage = new Message();
        replyMessage.setType(MessageType.JOIN_SUPER_PEER_OK);

        PeerType newNodeType = null;
        try {
            newNodeType = PeerType.valueOf(message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_TYPE));
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown peer type", e);
        }

        String newNodeIP = message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP);
        int newNodePort = Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT));

        if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
            if (serviceHolder.getRouter().getRoutingTable() instanceof SuperPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable =
                        (SuperPeerRoutingTable) serviceHolder.getRouter().getRoutingTable();

                if (newNodeType == PeerType.ORDINARY_PEER) {
                    if (superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(newNodeIP, newNodePort) !=
                            null || superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes().size() <
                            serviceHolder.getConfiguration().getMaxAssignedOrdinaryPeerCount()) {
                        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(newNodeIP, newNodePort);

                        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
                        if (resourceIndex instanceof SuperPeerResourceIndex) {
                            SuperPeerResourceIndex superPeerResourceIndex = ((SuperPeerResourceIndex) resourceIndex);

                            int resourceCount = Integer.parseInt(message.getData(
                                    MessageIndexes.JOIN_SUPER_PEER_RESOURCE_COUNT));
                            for (int i = 0; i < resourceCount; i++) {
                                superPeerResourceIndex.addAggregatedResource(
                                        message.getData(MessageIndexes.JOIN_SUPER_PEER_RESOURCE_START_INDEX + i),
                                        newNodeIP, newNodePort
                                );
                            }
                        } else {
                            logger.warn("Inconsistent ordinary resource index in super peer type");
                        }

                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                                MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS);
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP,
                                serviceHolder.getConfiguration().getIp());
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT,
                                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                    } else {
                        List<Node> superPeers =
                                new ArrayList<>(superPeerRoutingTable.getAllSuperPeerNetworkNodes());
                        superPeers.removeAll(fullSuperPeerCache);

                        if (superPeers.size() > 0) {
                            int selectedIndex = ThreadLocalRandom.current().nextInt(0, superPeers.size());
                            Node newSuperPeer = superPeers.get(selectedIndex);

                            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                                    MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL);
                            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP,
                                    serviceHolder.getConfiguration().getIp());
                            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT,
                                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_NEW_IP,
                                    newSuperPeer.getIp());
                            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_NEW_PORT,
                                    Integer.toString(newSuperPeer.getPort()));
                        } else {
                            replyMessage.setData(
                                    MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                                    MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NO_ONE_ELSE
                            );
                            replyMessage.setData(
                                    MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP,
                                    serviceHolder.getConfiguration().getIp()
                            );
                            replyMessage.setData(
                                    MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT,
                                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort())
                            );
                        }
                    }
                } else if (newNodeType == PeerType.SUPER_PEER) {
                    superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(newNodeIP, newNodePort);

                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                            MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS);
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP,
                            serviceHolder.getConfiguration().getIp());
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT,
                            Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                } else {
                    logger.warn("Message dropped due to the requesting node belonging to unknown peer type");
                }
            } else {
                logger.warn("Inconsistent ordinary routing table in super peer type");
            }
        } else {
            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                    MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_NOT_SUPER_PEER);
        }
        serviceHolder.getRouter().sendMessage(newNodeIP, newNodePort, replyMessage);
    }

    /**
     * Handle JOIN_SUPER_PEER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinSuperPeerOkMessage(Node fromNode, Message message) {
        switch (message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE)) {
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_NOT_SUPER_PEER:
                searchForSuperPeer();
                break;
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL:
                fullSuperPeerCacheLock.lock();
                try {
                    fullSuperPeerCache.add(new Node(
                            message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP),
                            Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT))
                    ));
                } finally {
                    fullSuperPeerCacheLock.unlock();
                }

                String newNodeIP = message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_NEW_IP);
                int newNodePort = Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_NEW_PORT));
                if (fullSuperPeerCache.contains(new Node(newNodeIP, newNodePort))) {
                    selfPromoteSuperPeer(true);
                } else {
                    connectToSuperPeer(newNodeIP, newNodePort);
                }
                break;
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NO_ONE_ELSE:
                fullSuperPeerCacheLock.lock();
                try {
                    fullSuperPeerCache.add(new Node(
                            message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP),
                            Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT))
                    ));
                } finally {
                    fullSuperPeerCacheLock.unlock();
                }

                selfPromoteSuperPeer(true);
                break;
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS:
                cancelSearchForSuperPeer();

                String sourceIP = message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_IP);
                int sourcePort = Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_SOURCE_PORT));

                if (serviceHolder.getPeerType() == PeerType.ORDINARY_PEER) {
                    if (serviceHolder.getRouter().getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                        ((OrdinaryPeerRoutingTable) serviceHolder.getRouter().getRoutingTable())
                                .setAssignedSuperPeer(sourceIP, sourcePort);
                    } else {
                        logger.warn("Inconsistent super peer routing table in ordinary peer");
                    }
                } else {
                    if (serviceHolder.getRouter().getRoutingTable() instanceof SuperPeerRoutingTable) {
                        ((SuperPeerRoutingTable) serviceHolder.getRouter().getRoutingTable())
                                .addSuperPeerNetworkRoutingTableEntry(sourceIP, sourcePort);
                    } else {
                        logger.warn("Inconsistent ordinary peer routing table in super peer");
                    }
                }
                break;
            default:
                logger.warn("Unknown " + MessageType.JOIN_SUPER_PEER_OK.getValue() + " message value in message "
                        + message.toString());
        }
    }

    /**
     * Handle LIST_RESOURCES type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListResourcesMessage(Node fromNode, Message message) {
        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LIST_RESOURCES_OK);
        replyMessage.setData(MessageIndexes.LIST_RESOURCES_OK_IP,
                serviceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.LIST_RESOURCES_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

        List<OwnedResource> ownedResources = new ArrayList<>(serviceHolder.getResourceIndex().getAllOwnedResources());
        replyMessage.setData(MessageIndexes.LIST_RESOURCES_OK_RESOURCE_COUNT, Integer.toString(ownedResources.size()));
        for (int i = 0; i < ownedResources.size(); i++) {
            replyMessage.setData(MessageIndexes.LIST_RESOURCES_OK_RESOURCE_START_INDEX + i,
                    ownedResources.get(i).getName());
        }

        serviceHolder.getRouter().sendMessage(
                message.getData(MessageIndexes.LIST_RESOURCES_IP),
                Integer.parseInt(message.getData(MessageIndexes.LIST_RESOURCES_PORT)),
                replyMessage
        );
    }

    /**
     * Handle LIST_RESOURCES_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListResourcesOkMessage(Node fromNode, Message message) {
        if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
            RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
            if (routingTable instanceof SuperPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
                ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
                if (resourceIndex instanceof SuperPeerResourceIndex) {
                    SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
                    Node node = superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(
                            message.getData(MessageIndexes.LIST_RESOURCES_OK_IP),
                            Integer.parseInt(message.getData(MessageIndexes.LIST_RESOURCES_OK_PORT))
                    );
                    if (node != null) {
                        int resourceCount = Integer.parseInt(message.getData(
                                MessageIndexes.LIST_RESOURCES_OK_RESOURCE_COUNT));
                        List<String> resourceNames = new ArrayList<>();
                        for (int i = 0; i < resourceCount; i++) {
                            resourceNames.add(message.getData(
                                    MessageIndexes.LIST_RESOURCES_OK_RESOURCE_START_INDEX + i));
                        }
                        superPeerResourceIndex.removeNodeFromAggregatedResources(node.getIp(), node.getPort());
                        superPeerResourceIndex.addAllAggregatedResources(resourceNames, node.getIp(), node.getPort());
                    } else {
                        logger.warn("Dropped message " + message.toString()
                                + " received from an unassigned ordinary peer");
                    }
                } else {
                    logger.error("Inconsistent ordinary peer resource index in super peer");
                }
            } else {
                logger.error("Inconsistent ordinary peer routing table in super peer");
            }
        } else {
            logger.warn(message.toString() + " Cannot be handled by ordinary peer");
        }
    }

    /**
     * Handle LIST_UNSTRUCTURED_CONNECTIONS type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListUnstructuredConnectionsMessage(Node fromNode, Message message) {
        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LIST_UNSTRUCTURED_CONNECTIONS_OK);
        replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_IP,
                serviceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

        List<Node> unstructuredNodes =
                new ArrayList<>(serviceHolder.getRouter().getRoutingTable().getAllUnstructuredNetworkNodes());

        if (unstructuredNodes.size() > 0) {
            int selectedNodesCount = (unstructuredNodes.size()
                    > MessageConstants.LIST_UNSTRUCTURED_CONNECTIONS_MAX_COUNT)
                    ? MessageConstants.LIST_UNSTRUCTURED_CONNECTIONS_MAX_COUNT : unstructuredNodes.size();

            replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_COUNT,
                    Integer.toString(selectedNodesCount));

            // Selecting random nodes
            for (int i = 0; i < selectedNodesCount * 2; i += 2) {
                Node node = unstructuredNodes.get(ThreadLocalRandom.current().nextInt(0, unstructuredNodes.size()));
                unstructuredNodes.remove(node);
                replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i,
                        node.getIp());
                replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i + 1,
                        Integer.toString(node.getPort()));
            }
        } else {
            replyMessage.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_COUNT, "0");
        }

        serviceHolder.getRouter().sendMessage(
                message.getData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_IP),
                Integer.parseInt(message.getData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_PORT)),
                replyMessage
        );
    }

    /**
     * Handle LIST_UNSTRUCTURED_CONNECTIONS_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListUnstructuredConnectionsOkMessage(Node fromNode, Message message) {
        int nodesCount = Integer.parseInt(
                message.getData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_COUNT)
        );

        Set<Node> newNodes = new HashSet<>();
        for (int i = 0; i < nodesCount * 2; i += 2) {
            String ip =
                    message.getData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i);
            int port = Integer.parseInt(
                    message.getData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i + 1)
            );
            newNodes.add(new Node(ip, port));
        }
        join(newNodes);
    }

    /**
     * Handle LIST_SUPER_PEER_CONNECTIONS type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListSuperPeerConnectionsMessage(Node fromNode, Message message) {
        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LIST_SUPER_PEER_CONNECTIONS_OK);
        replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_IP,
                serviceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

        RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
        if (routingTable instanceof SuperPeerRoutingTable) {
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;

            List<Node> superPeerNodes =
                    new ArrayList<>(superPeerRoutingTable.getAllSuperPeerNetworkNodes());
            if (superPeerNodes.size() > 0) {
                int selectedNodesCount = (superPeerNodes.size()
                        > MessageConstants.LIST_SUPER_PEER_CONNECTIONS_MAX_COUNT)
                        ? MessageConstants.LIST_SUPER_PEER_CONNECTIONS_MAX_COUNT : superPeerNodes.size();

                replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_COUNT,
                        Integer.toString(selectedNodesCount));

                // Selecting random nodes
                for (int i = 0; i < selectedNodesCount * 2; i += 2) {
                    Node node = superPeerNodes.get(ThreadLocalRandom.current().nextInt(0, superPeerNodes.size()));
                    superPeerNodes.remove(node);
                    replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i,
                            node.getIp());
                    replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i + 1,
                            Integer.toString(node.getPort()));
                }
            } else {
                replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_COUNT, "0");
            }
        } else {
            replyMessage.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_COUNT,
                    MessageConstants.LIST_SUPER_PEER_CONNECTIONS_NOT_SUPER_PEER);
        }

        serviceHolder.getRouter().sendMessage(
                message.getData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_IP),
                Integer.parseInt(message.getData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_PORT)),
                replyMessage
        );
    }

    /**
     * Handle LIST_SUPER_PEER_CONNECTIONS_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleListSuperPeerConnectionsOkMessage(Node fromNode, Message message) {
        String nodesCountString = message.getData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_COUNT);

        if (!Objects.equals(nodesCountString, MessageConstants.LIST_SUPER_PEER_CONNECTIONS_NOT_SUPER_PEER)) {
            int nodeCount = Integer.parseInt(nodesCountString);
            for (int i = 0; i < nodeCount * 2; i += 2) {
                String ip =
                        message.getData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i);
                int port = Integer.parseInt(
                        message.getData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_OK_CONNECTIONS_START_INDEX + i + 1)
                );
                connectToSuperPeer(ip, port);
            }
        }
    }

    /**
     * Request the owned resources list from the assigned ordinary peers.
     * This will not be called by ordinary peers.
     *
     * @param superPeerRoutingTable The super peer routing table used by the current node
     */
    private void requestOwnedResourcesList(SuperPeerRoutingTable superPeerRoutingTable) {
        Set<Node> nodes = superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes();
        nodes.forEach(node -> {
            Message listResourcesMessage = new Message();
            listResourcesMessage.setType(MessageType.LIST_RESOURCES);
            listResourcesMessage.setData(MessageIndexes.LIST_RESOURCES_IP,
                    serviceHolder.getConfiguration().getIp());
            listResourcesMessage.setData(MessageIndexes.LIST_RESOURCES_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

            logger.info("Requesting to list owned resource from node " + node.toString());
            serviceHolder.getRouter().sendMessage(node, listResourcesMessage);
        });
    }

    /**
     * Request to list the unstructured connections.
     *
     * @param nodes The nodes to which the request should be sent
     */
    private void requestUnstructuredConnections(Set<Node> nodes) {
        nodes.forEach(node -> {
            Message listUnstructuredConnections = new Message();
            listUnstructuredConnections.setType(MessageType.LIST_UNSTRUCTURED_CONNECTIONS);
            listUnstructuredConnections.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_IP,
                    serviceHolder.getConfiguration().getIp());
            listUnstructuredConnections.setData(MessageIndexes.LIST_UNSTRUCTURED_CONNECTIONS_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

            logger.info("Requesting to list unstructured connections from node " + node.toString());
            serviceHolder.getRouter().sendMessage(node, listUnstructuredConnections);
        });
    }

    /**
     * Request to list the super peer connections.
     *
     * @param nodes The nodes to which the request should be sent
     */
    private void requestSuperPeerConnections(Set<Node> nodes) {
        nodes.forEach(node -> {
            Message listUnstructuredConnections = new Message();
            listUnstructuredConnections.setType(MessageType.LIST_SUPER_PEER_CONNECTIONS);
            listUnstructuredConnections.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_IP,
                    serviceHolder.getConfiguration().getIp());
            listUnstructuredConnections.setData(MessageIndexes.LIST_SUPER_PEER_CONNECTIONS_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));

            logger.info("Requesting to list super peer connections from node " + node.toString());
            serviceHolder.getRouter().sendMessage(node, listUnstructuredConnections);
        });
    }
}
