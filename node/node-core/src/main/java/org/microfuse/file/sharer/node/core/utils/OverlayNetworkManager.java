package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageConstants;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Overlay Network Manager.
 */
public class OverlayNetworkManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(OverlayNetworkManager.class);

    private ServiceHolder serviceHolder;

    private final Lock gossipingLock;
    private Router router;
    private Thread gossipingThread;
    private boolean gossipingEnabled;
    private Timer serSuperPeerTimeoutTimer;

    public OverlayNetworkManager(Router router, ServiceHolder serviceHolder) {
        gossipingLock = new ReentrantLock();
        gossipingEnabled = false;

        serSuperPeerTimeoutTimer = new Timer(true);

        this.serviceHolder = serviceHolder;
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        logger.debug("Received message " + message.toString() + " from node " + fromNode.toString());
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
            default:
                logger.debug("Message " + message.toString() + " of unrecognized type ignored ");
        }
    }

    @Override
    public void onMessageSendFailed(Node toNode, Message message) {

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
                        gossip();
                        try {
                            Thread.sleep(serviceHolder.getConfiguration().getHeartbeatInterval());
                        } catch (InterruptedException e) {
                            logger.debug("Failed to sleep gossiping thread", e);
                        }
                    }
                    logger.debug("Stopped gossiping");
                });
                gossipingThread.setPriority(Thread.MIN_PRIORITY);
                gossipingThread.setDaemon(true);
                gossipingThread.start();
                logger.debug("Started gossiping");
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
        if (router.getRoutingTable().getAllUnstructuredNetworkRoutingTableNodes().size() <=
                serviceHolder.getConfiguration().getMaxUnstructuredPeerCount()) {
            logger.debug("Gossiping to grow the network");
            // TODO : Implement gossiping.
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
        regMessage.setData(MessageIndexes.REG_USERNAME, serviceHolder.getConfiguration().getUsername());
        router.sendMessageToBootstrapServer(regMessage);
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
        unregMessage.setData(MessageIndexes.UNREG_USERNAME, serviceHolder.getConfiguration().getUsername());
        router.sendMessageToBootstrapServer(unregMessage);
    }

    /**
     * Join the system.
     *
     * @param nodes The nodes to connect to
     */
    private void join(List<Node> nodes) {
        nodes.forEach(node -> {
            Message joinMessage = new Message();
            joinMessage.setType(MessageType.JOIN);
            joinMessage.setData(MessageIndexes.JOIN_IP, serviceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(node, joinMessage);
        });
    }

    /**
     * Leave the system.
     */
    public void leave() {
        router.getRoutingTable().getAll().forEach(node -> {
            Message leaveMessage = new Message();
            leaveMessage.setType(MessageType.LEAVE);
            leaveMessage.setData(MessageIndexes.LEAVE_IP, serviceHolder.getConfiguration().getIp());
            leaveMessage.setData(MessageIndexes.LEAVE_PORT,
                    Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(node, leaveMessage);
        });
    }

    /**
     * Heartbeat to all nodes.
     */
    public void heartBeat() {
        router.enableHeartBeat();
    }

    /**
     * Enable heart beating.
     */
    public void enableHeartBeat() {
        router.enableHeartBeat();
    }

    /**
     * Enable heart beating.
     */
    public void disableHeartBeat() {
        router.disableHeartBeat();
    }

    /**
     * Search for a super peer in the system.
     */
    private void searchForSuperPeer() {
        RoutingTable routingTable = router.getRoutingTable();
        if (routingTable instanceof OrdinaryPeerRoutingTable) {
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
            if (ordinaryPeerRoutingTable.getAssignedSuperPeer() == null) {
                Message searchSuperPeerMessage = new Message();
                searchSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP,
                        serviceHolder.getConfiguration().getIp());
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT,
                        Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                        Integer.toString(NodeConstants.INITIAL_HOP_COUNT));
                router.route(searchSuperPeerMessage);

                serSuperPeerTimeoutTimer.schedule(new TimerTask() {
                    public void run() {
                        selfAssignSuperPeer();
                    }
                }, serviceHolder.getConfiguration().getSerSuperPeerTimeout());
            }
        }
    }

    /**
     * Cancel the searching for super peers.
     */
    public void cancelSearchForSuperPeer() {
        serSuperPeerTimeoutTimer.cancel();
        serSuperPeerTimeoutTimer.purge();
    }

    /**
     *
     * Self assign current node as super peer.
     */
    private void selfAssignSuperPeer() {
        serviceHolder.promoteToSuperPeer();
        // TODO : Implement self assigning super peer. (Announce to others ?)
    }

    /**
     * Connect to a super peer.
     */
    private void connectToSuperPeer(String ip, int port) {
        Message joinMessage = new Message();
        joinMessage.setType(MessageType.JOIN_SUPER_PEER);
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_TYPE, serviceHolder.getPeerType().toString());
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP, serviceHolder.getConfiguration().getIp());
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
        router.sendMessage(ip, port, joinMessage);
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
                logger.debug("Changing peer listening port to "
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
                if (nodesCount > 0) {
                    List<Node> nodesList = new ArrayList<>();
                    if (nodesCount <= 2) {
                        for (int i = 0; i < nodesCount * 2; i += 2) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START + i;
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex + 1));
                            nodesList.add(node);
                        }
                    } else {
                        for (int i = 0; i < 2; ) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                                    + ThreadLocalRandom.current().nextInt(0, nodesCount);
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex + 1));

                            if (!nodesList.contains(node)) {
                                nodesList.add(node);
                                i++;
                            }
                        }
                    }

                    if (nodesList.size() > 0) {
                        join(nodesList);
                    } else {
                        selfAssignSuperPeer();
                    }
                } else {
                    selfAssignSuperPeer();
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
            logger.debug("Unknown value " + message.getData(MessageIndexes.UNREG_OK_VALUE) + " in message \""
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
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.JOIN_IP));
        node.setPort(message.getData(MessageIndexes.JOIN_PORT));
        node.setAlive(true);

        boolean isSuccessful = router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node);
        String superPeerNodeIP = null;
        int superPeerNodePort = -1;

        if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
            superPeerNodeIP = serviceHolder.getConfiguration().getIp();
            superPeerNodePort = serviceHolder.getConfiguration().getPeerListeningPort();
        } else {
            if (router.getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) router.getRoutingTable();

                if (ordinaryPeerRoutingTable.getAssignedSuperPeer().isAlive()) {
                    superPeerNodeIP = ordinaryPeerRoutingTable.getAssignedSuperPeer().getIp();
                    superPeerNodePort = ordinaryPeerRoutingTable.getAssignedSuperPeer().getPort();
                } else {
                    logger.debug("Assigned super peer is dead");
                }
            } else {
                logger.warn("Inconsistent ordinary peer type and super peer routing table");
            }
        }

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.JOIN_OK);
        replyMessage.setData(
                MessageIndexes.JOIN_OK_VALUE,
                (isSuccessful ? MessageConstants.JOIN_OK_VALUE_SUCCESS : MessageConstants.JOIN_OK_VALUE_ERROR)
        );
        replyMessage.setData(MessageIndexes.JOIN_OK_IP, serviceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.JOIN_OK_PORT,
                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
        if (superPeerNodeIP != null && superPeerNodePort >= 1) {
            replyMessage.setData(MessageIndexes.JOIN_OK_SUPER_PEER_IP, superPeerNodeIP);
            replyMessage.setData(MessageIndexes.JOIN_OK_SUPER_PEER_PORT, Integer.toString(superPeerNodePort));
        }
        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle JOIN_OK type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE), MessageConstants.JOIN_OK_VALUE_SUCCESS)) {
            Node node = new Node();
            node.setIp(message.getData(MessageIndexes.JOIN_OK_IP));
            node.setPort(message.getData(MessageIndexes.JOIN_OK_PORT));

            RoutingTable routingTable = router.getRoutingTable();
            routingTable.addUnstructuredNetworkRoutingTableEntry(node);

            if (routingTable instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;

                if (ordinaryPeerRoutingTable.getAssignedSuperPeer() == null) {
                    String superPeerIP = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_IP);
                    String superPeerPort = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_PORT);

                    if (superPeerIP != null && superPeerPort != null) {
                        if (router.getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                            Node superPeerNode = new Node();
                            superPeerNode.setIp(superPeerIP);
                            superPeerNode.setPort(superPeerPort);
                            routingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode);

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
                MessageConstants.JOIN_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.JOIN_OK_VALUE) + " in message \""
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
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.LEAVE_IP));
        node.setPort(message.getData(MessageIndexes.LEAVE_PORT));
        node.setAlive(false);

        boolean isSuccessful = router.getRoutingTable().removeFromAll(node);

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LEAVE_OK);
        replyMessage.setData(
                MessageIndexes.LEAVE_OK_VALUE,
                (isSuccessful ? MessageConstants.LEAVE_OK_VALUE_SUCCESS : MessageConstants.LEAVE_OK_VALUE_ERROR)
        );
        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle LEAVE_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE), MessageConstants.LEAVE_OK_VALUE_SUCCESS)) {
            router.getRoutingTable().removeFromAll(fromNode);
        } else if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE),
                MessageConstants.LEAVE_OK_VALUE_ERROR)) {
            logger.warn("Failed to disconnect unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.LEAVE_OK_VALUE) + " in message \""
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
        if (!Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_IP) &&
                !Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT),
                        MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_PORT)) {
            connectToSuperPeer(
                    message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                    Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT))
            );
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

        String newAssignedNodeIP = message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP);
        int newAssignedNodePort = Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT));

        if (serviceHolder.getPeerType() == PeerType.SUPER_PEER) {
            if (router.getRoutingTable() instanceof SuperPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) router.getRoutingTable();

                if (newNodeType == PeerType.ORDINARY_PEER) {
                    if (superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes().size() <
                            serviceHolder.getConfiguration().getMaxAssignedOrdinaryPeerCount()) {
                        Node newAssignedNode = superPeerRoutingTable.get(newAssignedNodeIP, newAssignedNodePort);
                        if (newAssignedNode == null) {
                            newAssignedNode = new Node();
                            newAssignedNode.setIp(newAssignedNodeIP);
                            newAssignedNode.setPort(newAssignedNodePort);
                        }

                        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(newAssignedNode);

                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                                MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS);
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_IP,
                                serviceHolder.getConfiguration().getIp());
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_PORT,
                                Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort()));
                    } else {
                        Node newSuperPeer;
                        List<Node> superPeers =
                                new ArrayList<>(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes());
                        if (superPeers.size() > 0) {
                            int selectedIndex = ThreadLocalRandom.current().nextInt(0, superPeers.size());
                            newSuperPeer = superPeers.get(selectedIndex);
                        } else {
                            newSuperPeer = new Node();
                            newSuperPeer.setIp(MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NOT_FOUND_IP);
                            newSuperPeer.setPort(MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NOT_FOUND_PORT);
                        }

                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                                MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL);
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_IP,
                                newSuperPeer.getIp());
                        replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_PORT,
                                Integer.toString(newSuperPeer.getPort()));
                    }
                } else if (newNodeType == PeerType.SUPER_PEER) {
                    Node newSuperPeerNode = superPeerRoutingTable.get(newAssignedNodeIP, newAssignedNodePort);
                    if (newSuperPeerNode == null) {
                        newSuperPeerNode = new Node();
                        newSuperPeerNode.setIp(newAssignedNodeIP);
                        newSuperPeerNode.setPort(newAssignedNodePort);
                    }

                    superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(newSuperPeerNode);

                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                            MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS);
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_IP,
                            serviceHolder.getConfiguration().getIp());
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_PORT,
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
        router.sendMessage(newAssignedNodeIP, newAssignedNodePort, replyMessage);
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
                String ip = message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_IP);
                String port = message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_PORT);
                if (Objects.equals(ip, MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NOT_FOUND_IP) ||
                        Objects.equals(port, MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NOT_FOUND_PORT)) {
                    searchForSuperPeer();
                } else {
                    connectToSuperPeer(ip, Integer.parseInt(port));
                }
                break;
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS:
                String superPeerIP = message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_IP);
                int superPeerPort = Integer.parseInt(message.getData(
                        MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_PORT));

                if (serviceHolder.getPeerType() == PeerType.ORDINARY_PEER) {
                    if (router.getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                        OrdinaryPeerRoutingTable ordinaryPeerRoutingTable =
                                (OrdinaryPeerRoutingTable) router.getRoutingTable();

                        Node superPeerNode = ordinaryPeerRoutingTable.get(superPeerIP, superPeerPort);
                        if (superPeerNode == null) {
                            superPeerNode = new Node();
                            superPeerNode.setIp(superPeerIP);
                            superPeerNode.setPort(superPeerPort);
                        }
                        ordinaryPeerRoutingTable.setAssignedSuperPeer(superPeerNode);
                    } else {
                        logger.warn("Inconsistent super peer routing table in ordinary peer");
                    }
                } else {
                    if (router.getRoutingTable() instanceof SuperPeerRoutingTable) {
                        SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) router.getRoutingTable();

                        Node newSuperPeerNode = superPeerRoutingTable.get(superPeerIP, superPeerPort);
                        if (newSuperPeerNode == null) {
                            newSuperPeerNode = new Node();
                            newSuperPeerNode.setIp(superPeerIP);
                            newSuperPeerNode.setPort(superPeerPort);
                        }

                        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(newSuperPeerNode);
                    } else {
                        logger.warn("Inconsistent ordinary peer routing table in super peer");
                    }
                }
                cancelSearchForSuperPeer();
                break;
            default:
                logger.warn("Unknown " + MessageType.JOIN_SUPER_PEER_OK.getValue() + " message value in message "
                        + message.toString());
        }
    }
}
