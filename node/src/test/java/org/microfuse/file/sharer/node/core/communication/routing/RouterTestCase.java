package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.microfuse.file.sharer.node.core.utils.MessageConstants;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
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
        networkHandler = Mockito.mock(NetworkHandler.class);
        routingStrategy = Mockito.mock(RoutingStrategy.class);
        router = Mockito.spy(new Router(networkHandler, routingStrategy));

        fromNode = new Node();
        fromNode.setIp("192.168.1.2");
        fromNode.setPort(4532);

        sourceNode = new Node();
        sourceNode.setIp("192.168.1.3");
        sourceNode.setPort(6534);

        serMessage = Message.parse("0049 " + MessageType.SER.getValue() + " " + sourceNode.getIp()
                + " " + sourceNode.getPort() + " \"Lord of the Rings\" "
                + Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        serSuperPeerMessage = Message.parse("0036 " + MessageType.SER_SUPER_PEER.getValue() + " " + sourceNode.getIp()
                + " " + sourceNode.getPort() + " "
                + Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        RoutingTable routingTable = router.getRoutingTable();
        spyRoutingTable = Mockito.spy(routingTable);
        Whitebox.setInternalState(router, "routingTable", spyRoutingTable);
    }

    @AfterMethod
    public void cleanup() {
        router.shutdown();
    }

    @Test
    public void testRestart() {
        router.restart();

        Mockito.verify(networkHandler, Mockito.times(1)).restart();
        Mockito.verify(spyRoutingTable, Mockito.times(1)).clear();
    }

    @Test
    public void testSendMessage() {
        Node toNode = Mockito.mock(Node.class);
        Mockito.when(toNode.getIp()).thenReturn("192.168.1.2");
        Mockito.when(toNode.getPort()).thenReturn(4532);

        router.sendMessage(toNode, serMessage);

        Mockito.verify(networkHandler, Mockito.times(1)).sendMessage(toNode.getIp(), toNode.getPort(), serMessage);
    }

    @Test
    public void testRoute() {
        router.route(serMessage);

        Mockito.verify(routingStrategy, Mockito.times(1)).getForwardingNodes(spyRoutingTable, null, serMessage);
    }

    @Test
    public void testOnMessageReceivedWithNonSerTypeMessage() {
        serMessage.setType(MessageType.REG);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Node usedNode = new Node();
        usedNode.setIp(fromNode.getIp());
        usedNode.setPort(fromNode.getPort());

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(1)).runTasksOnMessageReceived(usedNode, usedMessage);
    }

    @Test
    public void testOnSerMessageReceived() {
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
    }

    @Test
    public void testOnSerMessageReceivedWithResourceInOwnedResources() {
        OwnedResource ownedResource = new OwnedResource(serMessage.getData(MessageIndexes.SER_FILE_NAME));
        ServiceHolder.getResourceIndex().addResourceToIndex(ownedResource);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message message = Message.parse("0047 SEROK 1 " + ServiceHolder.getConfiguration().getIp()
                + " " + Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort())
                + " \"" + ownedResource.getName() + "\"");

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), message);
    }

    @Test
    public void testOnSerMessageReceivedWithResourceNotInOwnedResourcesWithHopCountLessThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Mockito.verify(routingStrategy, Mockito.times(1)).getForwardingNodes(spyRoutingTable, fromNode, serMessage);
        Mockito.verify(networkHandler, Mockito.times(1)).sendMessage(node.getIp(), node.getPort(), serMessage);
    }

    @Test
    public void testOnSerMessageReceivedWithResourceNotInOwnedResourcesWithHopCountHigherThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(Constants.DEFAULT_TIME_TO_LIVE));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse("0049 " + MessageType.SER_OK.getValue()
                + " " + MessageConstants.SER_OK_NOT_FOUND_FILE_COUNT
                + " " + MessageConstants.SER_OK_NOT_FOUND_IP
                + " " + MessageConstants.SER_OK_NOT_FOUND_PORT);

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), usedMessage);
    }

    @Test
    public void testOnMessageSendFailedInUnstructuredNetworkOnly() {
        fromNode = Mockito.spy(fromNode);
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), serMessage);

        Message usedMessage = Message.parse(serMessage.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testOnMessageSendFailedInSuperPeerNetworkOnly() {
        ServiceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable());
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
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testOnMessageSendFailedInAssignedOrdinaryPeersNetworkOnly() {
        ServiceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable());
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
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testOnSerSuperPeerMessageReceived() {
        serSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);

        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Message usedMessage = Message.parse(serSuperPeerMessage.toString());
        usedMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(fromNode, usedMessage);
    }

    @Test
    public void testOnSerSuperPeerMessageReceivedToSuperPeer() {
        ServiceHolder.promoteToSuperPeer();

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Message message = Message.parse("0034 " + MessageType.SER_SUPER_PEER_OK.getValue()
                + " " + ServiceHolder.getConfiguration().getIp()
                + " " + Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), message);
    }

    @Test
    public void testOnSerSuperPeerMessageReceivedWithHopCountLessThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Mockito.verify(routingStrategy, Mockito.times(1))
                .getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage);
        Mockito.verify(networkHandler, Mockito.times(1)).sendMessage(node.getIp(), node.getPort(), serSuperPeerMessage);
    }

    @Test
    public void testOnSerSuperPeerMessageReceivedWithHopCountHigherThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        serSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                Integer.toString(Constants.DEFAULT_TIME_TO_LIVE));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, serSuperPeerMessage))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), serSuperPeerMessage);

        Message usedMessage = Message.parse("0049 " + MessageType.SER_SUPER_PEER_OK.getValue()
                + " " + MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_IP
                + " " + MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_PORT);

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), usedMessage);
    }

    @Test
    public void testPromoteToSuperPeerInOrdinaryPeer() {
        Node node = Mockito.mock(Node.class);
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        router.promoteToSuperPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertFalse(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes()).get(0),
                node);
    }

    @Test
    public void testDemoteToSuperPeerInSuperPeer() {
        Node node = Mockito.mock(Node.class);
        router.promoteToSuperPeer();
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        router.demoteToOrdinaryPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertFalse(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes()).get(0),
                node);
    }

    @Test
    public void testPromoteToSuperPeerInSuperPeer() {
        Node node = Mockito.mock(Node.class);
        router.promoteToSuperPeer();
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        router.promoteToSuperPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertTrue(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes()).get(0),
                node);
    }

    @Test
    public void testDemoteToSuperPeerInOrdinaryPeer() {
        Node node = Mockito.mock(Node.class);
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        router.demoteToOrdinaryPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertTrue(initialRoutingTable == finalRoutingTable);
        Assert.assertEquals(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes().size(), 1);
        Assert.assertEquals(new ArrayList<>(finalRoutingTable.getAllUnstructuredNetworkRoutingTableNodes()).get(0),
                node);
    }

    @Test
    public void testRunTasksOnMessageReceived() {
        RouterListener listener = Mockito.mock(RouterListener.class);
        Message message = Mockito.mock(Message.class);
        router.registerListener(listener);

        router.runTasksOnMessageReceived(fromNode, message);

        Mockito.verify(listener, Mockito.times(1)).onMessageReceived(fromNode, message);
    }
}
