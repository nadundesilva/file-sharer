package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.fromNode.core.communication.routing.Router class.
 */
public class RouterTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(RouterTestCase.class);

    private NetworkHandler networkHandler;
    private RoutingStrategy routingStrategy;
    private Router router;
    private RoutingTable spyRoutingTable;
    private Node fromNode;
    private Node sourceNode;
    private Message serMessage;
    private Message serSuperPeerMessage;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Router Test");

        networkHandler = Mockito.mock(NetworkHandler.class);
        routingStrategy = Mockito.mock(RoutingStrategy.class);
        router = Mockito.spy(new Router(networkHandler, routingStrategy, serviceHolder));

        fromNode = new Node();
        fromNode.setIp("192.168.1.2");
        fromNode.setPort(4532);

        sourceNode = new Node();
        sourceNode.setIp("192.168.1.3");
        sourceNode.setPort(6534);

        serMessage = Message.parse("0049 " + MessageType.SER.getValue() + " " + sourceNode.getIp()
                + " " + sourceNode.getPort() + " 0 "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1)
                + " \"Lord of the Rings\" ");

        serSuperPeerMessage = Message.parse("0036 " + MessageType.SER_SUPER_PEER.getValue() + " 0 " + sourceNode.getIp()
                + " " + sourceNode.getPort() + " "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));

        RoutingTable routingTable = router.getRoutingTable();
        spyRoutingTable = Mockito.spy(routingTable);
        Whitebox.setInternalState(router, "routingTable", spyRoutingTable);
    }

    @AfterMethod
    public void cleanup() {
        logger.info("Cleaning Up Router Test");

        router.shutdown();
    }

    @Test(priority = 1)
    public void testRestart() {
        logger.info("Running Router Test 01 - Restart");

        router.restart();

        Mockito.verify(networkHandler, Mockito.times(1)).restart();
        Mockito.verify(spyRoutingTable, Mockito.times(1)).clear();
    }

    @Test(priority = 1)
    public void testSendMessage() {
        logger.info("Running Router Test 02 - Send message");

        Node toNode = Mockito.mock(Node.class);
        Mockito.when(toNode.getIp()).thenReturn("192.168.1.2");
        Mockito.when(toNode.getPort()).thenReturn(4532);

        router.sendMessage(toNode, serMessage);

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(toNode.getIp(), toNode.getPort(), serMessage);
    }

    @Test(priority = 1)
    public void testRoute() {
        logger.info("Running Router Test 03 - Route");

        router.route(serMessage);

        Mockito.verify(routingStrategy, Mockito.times(1))
                .getForwardingNodes(spyRoutingTable, null, serMessage);
    }

    @Test(priority = 2)
    public void testOnMessageReceivedWithNonSerTypeMessage() {
        logger.info("Running Router Test 04 - On message received with non " + MessageType.SER.getValue()
                + " type message");

        serMessage.setType(MessageType.REG);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Node usedNode = new Node();
        usedNode.setIp(fromNode.getIp());
        usedNode.setPort(fromNode.getPort());

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(1))
                .runTasksOnMessageReceived(usedNode, usedMessage);
    }

    @Test(priority = 2)
    public void testOnSerMessageReceived() {
        logger.info("Running Router Test 05 - On " + MessageType.SER.getValue() + " message received");

        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
    }

    @Test(priority = 3)
    public void testOnSerMessageReceivedWithResourceInOwnedResources() {
        logger.info("Running Router Test 06 - On " + MessageType.SER.getValue()
                + " message received with resource in owned resources");

        String ownedResourceName = serMessage.getData(MessageIndexes.SER_QUERY);
        serviceHolder.getResourceIndex().addOwnedResource(ownedResourceName, null);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message message = Message.parse("0047 " + MessageType.SER_OK.getValue()
                + " \"" + serMessage.getData(MessageIndexes.SER_QUERY) + "\""
                + " 0 19 1"
                + " " + serviceHolder.getConfiguration().getIp()
                + " " + Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort())
                + " \"" + ownedResourceName + "\"");

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), message);
    }

    @Test(priority = 3)
    public void testOnSerMessageReceivedWithResourceNotInOwnedResourcesWithHopCountLessThanTimeToLive() {
        logger.info("Running Router Test 07 - On " + MessageType.SER.getValue()
                + " message received with resource not in owned resources with hop count less than time to live");

        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Mockito.verify(routingStrategy, Mockito.times(1))
                .getForwardingNodes(spyRoutingTable, fromNode, serMessage);
        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(node.getIp(), node.getPort(), serMessage);
    }

    @Test(priority = 3)
    public void testOnSerMessageReceivedWithResourceNotInOwnedResourcesWithHopCountHigherThanTimeToLive() {
        logger.info("Running Router Test 08 - On " + MessageType.SER.getValue()
                + " message received with resource not in owned resources with hop count higher than time to live");

        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Mockito.verify(networkHandler, Mockito.times(0))
                .sendMessage(Mockito.eq(sourceNode.getIp()), Mockito.eq(sourceNode.getPort()),
                        Mockito.any(Message.class));
    }

    @Test(priority = 2)
    public void testOnMessageSendFailedInUnstructuredNetworkOnly() {
        logger.info("Running Router Test 09 - On message send failed in unstructured network only");

        fromNode = Mockito.spy(fromNode);
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setState(NodeState.PENDING_INACTIVATION);
    }

    @Test(priority = 2)
    public void testOnMessageSendFailedInSuperPeerNetworkOnly() {
        logger.info("Running Router Test 10 - On message send failed in super peer network only");

        serviceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable(serviceHolder));
        Whitebox.setInternalState(router, "routingTable", superPeerRoutingTable);

        fromNode = Mockito.spy(fromNode);
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setState(NodeState.PENDING_INACTIVATION);
    }

    @Test(priority = 2)
    public void testOnMessageSendFailedInAssignedOrdinaryPeersNetworkOnly() {
        logger.info("Running Router Test 11 - On message send failed in assigned ordinary peers network only");

        serviceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable(serviceHolder));
        Whitebox.setInternalState(router, "routingTable", superPeerRoutingTable);

        fromNode = Mockito.spy(fromNode);
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setState(NodeState.PENDING_INACTIVATION);
    }

    @Test(priority = 3)
    public void testOnSerSuperPeerMessageReceived() {
        logger.info("Running Router Test 12 - On " + MessageType.SER_SUPER_PEER.getValue() + " message received");

        serSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);

        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Message usedMessage = Message.parse(serSuperPeerMessage.toString());
        usedMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
    }

    @Test(priority = 3)
    public void testOnSerSuperPeerMessageReceivedToSuperPeer() {
        logger.info("Running Router Test 13 - On " + MessageType.SER_SUPER_PEER.getValue()
                + " message received to super peer");

        serviceHolder.promoteToSuperPeer();

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Message message = Message.parse("0034 " + MessageType.SER_SUPER_PEER_OK.getValue()
                + " " + serviceHolder.getConfiguration().getIp()
                + " " + Integer.toString(serviceHolder.getConfiguration().getPeerListeningPort())
                + " 0 19");

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), message);
    }

    @Test(priority = 3)
    public void testOnSerSuperPeerMessageReceivedWithHopCountLessThanTimeToLive() {
        logger.info("Running Router Test 14 - On " + MessageType.SER_SUPER_PEER.getValue()
                + " message received with hop count less than time to live");

        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Mockito.verify(routingStrategy, Mockito.times(1))
                .getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage);
        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(node.getIp(), node.getPort(), serSuperPeerMessage);
    }

    @Test(priority = 3)
    public void testOnSerSuperPeerMessageReceivedWithHopCountHigherThanTimeToLive() {
        logger.info("Running Router Test 15 - On " + MessageType.SER_SUPER_PEER.getValue()
                + " message received with hop count higher than time to live");

        Set<Node> nodes = new HashSet<>();
        Node node = new Node("192.168.1.2", 6452);
        nodes.add(node);

        serSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Mockito.verify(networkHandler, Mockito.times(0))
                .sendMessage(Mockito.eq(sourceNode.getIp()), Mockito.eq(sourceNode.getPort()),
                        Mockito.any(Message.class));
    }

    @Test(priority = 1)
    public void testPromoteToSuperPeerInOrdinaryPeer() {
        logger.info("Running Router Test 16 - Promote to super peer in ordinary peer");

        Node node = new Node("192.168.1.2", 6452);

        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        router.promoteToSuperPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertFalse(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkNodes()).get(0),
                node);
    }

    @Test(priority = 1)
    public void testDemoteToSuperPeerInSuperPeer() {
        logger.info("Running Router Test 17 - Demote to super peer in super peer");

        Node node = new Node("192.168.1.2", 6452);

        router.promoteToSuperPeer();
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        router.demoteToOrdinaryPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertFalse(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkNodes()).get(0),
                node);
    }

    @Test(priority = 1)
    public void testPromoteToSuperPeerInSuperPeer() {
        logger.info("Running Router Test 18 - Promote to super peer in super peer");

        Node node = new Node("192.168.1.2", 6452);

        router.promoteToSuperPeer();
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        router.promoteToSuperPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertTrue(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkNodes()).get(0),
                node);
    }

    @Test(priority = 1)
    public void testDemoteToSuperPeerInOrdinaryPeer() {
        logger.info("Running Router Test 19 - Promote to super peer in ordinary peer");

        Node node = new Node("192.168.1.2", 6452);

        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        router.demoteToOrdinaryPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertTrue(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkNodes()).get(0),
                node);
    }

    @Test(priority = 1)
    public void testRunTasksOnMessageReceived() {
        logger.info("Running Router Test 20 - Run tasks on message received");

        RouterListener listener = Mockito.mock(RouterListener.class);
        Message message = Mockito.mock(Message.class);
        router.registerListener(listener);

        router.runTasksOnMessageReceived(fromNode, message);

        Mockito.verify(listener, Mockito.times(1)).onMessageReceived(fromNode, message);
    }

    @Test(priority = 1)
    public void testHeartbeat() {
        logger.info("Running Router Test 21 - Heartbeat");

        Node node = new Node("192.168.1.2", 6452);

        router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        Configuration configuration = serviceHolder.getConfiguration();
        configuration.setHeartbeatInterval(1000);

        Message message = Message.parse("0029 " + MessageType.HEARTBEAT.getValue() + " " + configuration.getIp() + " "
                + configuration.getPeerListeningPort());

        router.enableHeartBeat();
        waitFor(1200);

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(node.getIp(), node.getPort(), message);
    }

    @Test(priority = 1)
    public void testOnHeartbeatOkMessageReceived() {
        logger.info("Running Router Test 22 - On " + MessageType.HEARTBEAT_OK.getValue() + " message received");

        Node node = new Node("192.168.1.2", 6452);

        router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
        Configuration configuration = serviceHolder.getConfiguration();

        Message message = Message.parse("0029 " + MessageType.HEARTBEAT.getValue() + " " + node.getIp() + " "
                + node.getPort());

        router.onMessageReceived(node.getIp(), node.getPort(), message);

        Message replyMessage = Message.parse("0031 " + MessageType.HEARTBEAT_OK.getValue() + " "
                + configuration.getIp() + " " + configuration.getPeerListeningPort());

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(node.getIp(), node.getPort(), replyMessage);
    }

    @Test(priority = 1)
    public void testHeartbeatDisable() {
        logger.info("Running Router Test 23 - Heartbeat disable");

        Node node = new Node("192.168.1.2", 6452);

        router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());

        Configuration configuration = serviceHolder.getConfiguration();
        configuration.setHeartbeatInterval(1000);

        Message message = Message.parse("0029 " + MessageType.HEARTBEAT.getValue() + " " + configuration.getIp() + " "
                + configuration.getPeerListeningPort());

        router.enableHeartBeat();
        waitFor(1200);
        router.disableHeartBeat();
        waitFor(1200);

        Mockito.verify(networkHandler, Mockito.times(2)).sendMessage(node.getIp(), node.getPort(), message);
    }
}
