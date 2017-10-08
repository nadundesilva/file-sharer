package org.microfuse.file.sharer.node.core.communication.routing;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.ServiceHolder;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.microfuse.file.sharer.node.core.utils.MessageConstants;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
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
    private Message message;

    @BeforeMethod
    public void initializeMethod() {
        networkHandler = Mockito.mock(NetworkHandler.class);
        routingStrategy = Mockito.mock(RoutingStrategy.class);
        router = Mockito.spy(new Router(networkHandler, routingStrategy));

        fromNode = Mockito.mock(Node.class);
        Mockito.when(fromNode.getIp()).thenReturn("192.168.1.2");
        Mockito.when(fromNode.getPort()).thenReturn(4532);

        sourceNode = Mockito.mock(Node.class);
        Mockito.when(sourceNode.getIp()).thenReturn("192.168.1.3");
        Mockito.when(sourceNode.getPort()).thenReturn(6534);

        message = Message.parse("0049 SER " + sourceNode.getIp()
                + " " + sourceNode.getPort() + " \"Lord of the Rings\" "
                + Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        RoutingTable routingTable = router.getRoutingTable();
        spyRoutingTable = Mockito.spy(routingTable);
        Whitebox.setInternalState(router, "routingTable", spyRoutingTable);
    }

    @Test
    public void testSendMessage() {
        Node toNode = Mockito.mock(Node.class);
        Mockito.when(toNode.getIp()).thenReturn("192.168.1.2");
        Mockito.when(toNode.getPort()).thenReturn(4532);

        router.sendMessage(toNode, message);

        Mockito.verify(networkHandler, Mockito.times(1)).sendMessage(toNode.getIp(), toNode.getPort(), message);
    }

    @Test
    public void testRoute() {
        router.route(message);

        Mockito.verify(routingStrategy, Mockito.times(1)).getForwardingNodes(spyRoutingTable, null, message);
    }

    @Test
    public void testOnMessageReceived() {
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse(message.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(usedMessage);
    }

    @Test
    public void testOnMessageReceivedWithNonSerTypeMessage() {
        message.setType(MessageType.REG);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse(message.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT))));

        Mockito.verify(router, Mockito.times(1)).runTasksOnMessageReceived(usedMessage);
    }

    @Test
    public void testOnMessageReceivedWithResourceInOwnedResources() {
        OwnedResource ownedResource = new OwnedResource(message.getData(MessageIndexes.SER_FILE_NAME));
        ServiceHolder.getResourceIndex().addResourceToIndex(ownedResource);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), message);

        Message message = Message.parse("0049 SEROK 1 " + ServiceHolder.getConfiguration().getIp()
                + " " + Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort())
                + " " + Integer.toString(Constants.INITIAL_HOP_COUNT) + " \"" + ownedResource.getName() + "\"");

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), message);
    }

    @Test
    public void testOnMessageReceivedWithResourceNotInOwnedResourcesWithHopCountLessThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(Constants.DEFAULT_TIME_TO_LIVE - 1));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, message))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), message);

        Mockito.verify(networkHandler, Mockito.times(0)).sendMessage(node.getIp(), node.getPort(), message);
    }

    @Test
    public void testOnMessageReceivedWithResourceNotInOwnedResourcesWithHopCountHigherThanTimeToLive() {
        Set<Node> nodes = new HashSet<>();
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getIp()).thenReturn("192.168.1.5");
        Mockito.when(node.getPort()).thenReturn(7453);
        nodes.add(node);

        message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(Constants.DEFAULT_TIME_TO_LIVE));

        Mockito.when(routingStrategy.getForwardingNodes(spyRoutingTable, fromNode, message))
                .thenReturn(nodes);

        router.onMessageReceived(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse("0049 SEROK " + MessageConstants.SER_OK_NOT_FOUND_FILE_COUNT
                + " " + MessageConstants.SER_OK_NOT_FOUND_IP
                + " " + MessageConstants.SER_OK_NOT_FOUND_PORT);

        Mockito.verify(networkHandler, Mockito.times(1))
                .sendMessage(sourceNode.getIp(), sourceNode.getPort(), usedMessage);
    }

    @Test
    public void testOnMessageSendFailedInUnstructuredNetworkOnly() {
        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse(message.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testOnMessageSendFailedInSuperPeerNetworkOnly() {
        ServiceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable());
        Whitebox.setInternalState(router, "routingTable", superPeerRoutingTable);

        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse(message.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testOnMessageSendFailedInAssignedOrdinaryPeersNetworkOnly() {
        ServiceHolder.promoteToSuperPeer();
        router.promoteToSuperPeer();

        SuperPeerRoutingTable superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable());
        Whitebox.setInternalState(router, "routingTable", superPeerRoutingTable);

        Mockito.when(spyRoutingTable.getUnstructuredNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(null);
        Mockito.when(
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(fromNode.getIp(), fromNode.getPort()))
                .thenReturn(fromNode);

        router.onMessageSendFailed(fromNode.getIp(), fromNode.getPort(), message);

        Message usedMessage = Message.parse(message.toString());
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT,
                Integer.toString(Integer.parseInt(usedMessage.getData(MessageIndexes.SER_HOP_COUNT)) + 1));

        Mockito.verify(router, Mockito.times(0)).runTasksOnMessageReceived(usedMessage);
        Mockito.verify(fromNode, Mockito.times(1)).setAlive(false);
    }

    @Test
    public void testPromoteToSuperPeerInOrdinaryPeer() {
        Node node = Mockito.mock(Node.class);
        RoutingTable initialRoutingTable = router.getRoutingTable();
        initialRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        router.promoteToSuperPeer();
        RoutingTable finalRoutingTable = router.getRoutingTable();

        Assert.assertFalse(initialRoutingTable == finalRoutingTable);
        Assert.assertTrue(initialRoutingTable.getBootstrapServer() == finalRoutingTable.getBootstrapServer());
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
        Assert.assertTrue(initialRoutingTable.getBootstrapServer() == finalRoutingTable.getBootstrapServer());
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

        router.runTasksOnMessageReceived(message);

        Mockito.verify(listener, Mockito.times(1)).onMessageReceived(message);
    }
}
