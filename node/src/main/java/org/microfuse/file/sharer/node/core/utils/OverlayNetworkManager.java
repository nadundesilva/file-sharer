package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.routing.PeerType;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Overlay Network Manager.
 */
public class OverlayNetworkManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(OverlayNetworkManager.class);

    private Router router;

    private Thread gossipingThread;
    private boolean gossipingEnabled;

    private final Lock gossipingLock;

    public OverlayNetworkManager(Router router) {
        gossipingLock = new ReentrantLock();
        gossipingEnabled = false;

        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
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
                            Thread.sleep(ServiceHolder.getConfiguration().getHeartBeatInterval() * 1000);
                        } catch (InterruptedException e) {
                            logger.debug("Failed to sleep heartbeat thread", e);
                        }
                    }
                    logger.debug("Stopped Heart beating");
                });
                gossipingThread.setPriority(Thread.MIN_PRIORITY);
                gossipingThread.setDaemon(true);
                gossipingThread.start();
                logger.debug("Started Heart beating");
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
                ServiceHolder.getConfiguration().getMaxUnstructuredPeerCount()) {
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
        regMessage.setData(MessageIndexes.REG_IP, ServiceHolder.getConfiguration().getIp());
        regMessage.setData(MessageIndexes.REG_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
        regMessage.setData(MessageIndexes.REG_USERNAME, ServiceHolder.getConfiguration().getUsername());
        router.sendMessage(ServiceHolder.getConfiguration().getBootstrapServer(), regMessage);
    }

    /**
     * Unregister the current node from the bootstrap server.
     */
    public void unregister() {
        Message unregMessage = new Message();
        unregMessage.setType(MessageType.UNREG);
        unregMessage.setData(MessageIndexes.UNREG_IP, ServiceHolder.getConfiguration().getIp());
        unregMessage.setData(MessageIndexes.UNREG_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
        unregMessage.setData(MessageIndexes.UNREG_USERNAME, ServiceHolder.getConfiguration().getUsername());
        router.sendMessage(ServiceHolder.getConfiguration().getBootstrapServer(), unregMessage);
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
            joinMessage.setData(MessageIndexes.JOIN_IP, ServiceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
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
            leaveMessage.setData(MessageIndexes.LEAVE_IP, ServiceHolder.getConfiguration().getIp());
            leaveMessage.setData(MessageIndexes.LEAVE_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
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
        router.disableHeartBeat();
    }

    /**
     * Search for a super peer in the system.
     */
    private void searchForSuperPeer() {
        RoutingTable routingTable = router.getRoutingTable();
        if (routingTable instanceof OrdinaryPeerRoutingTable) {
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
            if (ordinaryPeerRoutingTable.getAssignedSuperPeer() != null) {
                Message searchSuperPeerMessage = new Message();
                searchSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP,
                        ServiceHolder.getConfiguration().getIp());
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT,
                        Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                        Integer.toString(Constants.INITIAL_HOP_COUNT));
                router.route(searchSuperPeerMessage);
            }
        }
    }

    /**
     * Self assign current node as super peer.
     */
    private void selfAssignSuperPeer() {
        // TODO : Implement self assigning super peer. (Announce to others ?)
    }

    /**
     * Connect to a super peer.
     */
    private void connectToSuperPeer(String ip, int port) {
        Message joinMessage = new Message();
        joinMessage.setType(MessageType.JOIN_SUPER_PEER);
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP, ServiceHolder.getConfiguration().getIp());
        joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
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
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_REGISTERED:
                logger.warn("Current node already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_OCCUPIED:
                logger.warn("Already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());

                // Retrying
                ServiceHolder.getConfiguration()
                        .setPeerListeningPort(ServiceHolder.getConfiguration().getPeerListeningPort() + 1);
                logger.debug("Changing peer listening port to "
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " and retrying");
                register();
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_FULL:
                logger.warn("Bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " full");
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
                            node.setPort(message.getData(messageIndex));
                            nodesList.add(node);
                        }
                    } else {
                        for (int i = 0; i < 2;) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                                    + ThreadLocalRandom.current().nextInt(0, nodesCount);
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex));

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

        if (ServiceHolder.getPeerType() == PeerType.SUPER_PEER) {
            superPeerNodeIP = ServiceHolder.getConfiguration().getIp();
            superPeerNodePort = ServiceHolder.getConfiguration().getPeerListeningPort();
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
        replyMessage.setData(MessageIndexes.JOIN_OK_IP, ServiceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.JOIN_OK_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
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

            router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node);

            String superPeerIP = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_IP);
            String superPeerPort = message.getData(MessageIndexes.JOIN_OK_SUPER_PEER_PORT);

            if (superPeerIP != null && superPeerPort != null) {
                if (router.getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                    OrdinaryPeerRoutingTable ordinaryPeerRoutingTable =
                            (OrdinaryPeerRoutingTable) router.getRoutingTable();

                    Node superPeerNode = ordinaryPeerRoutingTable.get(superPeerIP, Integer.parseInt(superPeerPort));
                    if (superPeerNode == null) {
                        superPeerNode = new Node();
                        superPeerNode.setIp(superPeerIP);
                        superPeerNode.setPort(superPeerPort);
                    }
                    ordinaryPeerRoutingTable.setAssignedSuperPeer(superPeerNode);
                } else {
                    logger.warn("Inconsistent super peer routing table in new node");
                }
            } else {
                searchForSuperPeer();
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
        if (Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_IP) ||
                Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT),
                        MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_PORT)) {
            selfAssignSuperPeer();
        } else {
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
        if (ServiceHolder.getPeerType() == PeerType.SUPER_PEER) {
            if (router.getRoutingTable() instanceof SuperPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) router.getRoutingTable();
                if (superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes().size() <=
                        ServiceHolder.getConfiguration().getMaxAssignedOrdinaryPeerCount()) {
                    replyMessage = new Message();
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                            MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS);
                } else {
                    List<Node> assignedOrdinaryPeers =
                            new ArrayList<>(superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes());
                    int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                            + ThreadLocalRandom.current().nextInt(0, assignedOrdinaryPeers.size());
                    Node newSuperPeer = assignedOrdinaryPeers.get(messageIndex);

                    replyMessage = new Message();
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                            MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL);
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_IP,
                            newSuperPeer.getIp());
                    replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_PORT,
                            Integer.toString(newSuperPeer.getPort()));
                }
            } else {
                logger.warn("Inconsistent ordinary routing table in super peer type");
            }
        } else {
            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                    MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_NOT_SUPER_PEER);
        }
        router.sendMessage(
                message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP),
                Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT)),
                replyMessage
        );
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
                connectToSuperPeer(
                        message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_IP),
                        Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_ERROR_FULL_NEW_PORT))
                );
                break;
            case MessageConstants.JOIN_SUPER_PEER_OK_VALUE_SUCCESS:
                if (ServiceHolder.getPeerType() == PeerType.ORDINARY_PEER) {
                    if (router.getRoutingTable() instanceof OrdinaryPeerRoutingTable) {
                        OrdinaryPeerRoutingTable ordinaryPeerRoutingTable =
                                (OrdinaryPeerRoutingTable) router.getRoutingTable();

                        Node superPeerNode = ordinaryPeerRoutingTable.get(
                                message.getData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_NEW_IP),
                                Integer.parseInt(message.getData(
                                        MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_NEW_PORT
                                ))
                        );
                        if (superPeerNode == null) {
                            superPeerNode = new Node();
                            superPeerNode.setIp(message.getData(
                                    MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_NEW_IP
                            ));
                            superPeerNode.setIp(message.getData(
                                    MessageIndexes.JOIN_SUPER_PEER_OK_VALUE_SUCCESS_NEW_PORT
                            ));
                        }
                        ordinaryPeerRoutingTable.setAssignedSuperPeer(superPeerNode);
                    } else {
                        logger.warn("Inconsistent super peer routing table in ordinary peer");
                    }
                } else {
                    logger.warn("Dropping message " + message.toString() + " since this is a super peer");
                }
                break;
            default:
                logger.warn("Unknown " + MessageType.JOIN_SUPER_PEER_OK.getValue() + " message value in message "
                        + message.toString());
        }
    }
}
