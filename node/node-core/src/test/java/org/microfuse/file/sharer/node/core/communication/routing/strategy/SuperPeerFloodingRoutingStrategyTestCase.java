package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.strategy.SuperPeerFloodingRoutingStrategy
 * class.
 */
public class SuperPeerFloodingRoutingStrategyTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerFloodingRoutingStrategyTestCase.class);

    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private SuperPeerRoutingTable superPeerRoutingTable;
    private SuperPeerFloodingRoutingStrategy superPeerFloodingRoutingStrategy;
    private String queryResourceName;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;
    private Node node5;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Super Peer Flooding Routing Strategy Test");

        superPeerFloodingRoutingStrategy = new SuperPeerFloodingRoutingStrategy(serviceHolder);

        ordinaryPeerRoutingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));
        superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable(serviceHolder));
        queryResourceName = "Lord of the Rings";

        fromNode = Mockito.mock(Node.class);
        Mockito.when(fromNode.isActive()).thenReturn(true);
        node1 = Mockito.mock(Node.class);
        Mockito.when(node1.isActive()).thenReturn(true);
        node2 = Mockito.mock(Node.class);
        Mockito.when(node2.isActive()).thenReturn(true);
        node3 = Mockito.mock(Node.class);
        Mockito.when(node3.isActive()).thenReturn(true);
        node4 = Mockito.mock(Node.class);
        Mockito.when(node4.isActive()).thenReturn(true);
        node5 = Mockito.mock(Node.class);
        Mockito.when(node5.isActive()).thenReturn(true);

        Set<Node> unstructuredNetworkNode = new HashSet<>();
        unstructuredNetworkNode.add(fromNode);
        unstructuredNetworkNode.add(node1);
        unstructuredNetworkNode.add(node2);
        unstructuredNetworkNode.add(node3);
        unstructuredNetworkNode.add(node4);
        unstructuredNetworkNode.add(node5);
        Mockito.when(ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes())
                .thenReturn(unstructuredNetworkNode);
        Mockito.when(superPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes())
                .thenReturn(unstructuredNetworkNode);

        Set<Node> assignedOrdinaryPeerNodes = new HashSet<>();
        assignedOrdinaryPeerNodes.add(fromNode);
        assignedOrdinaryPeerNodes.add(node1);
        assignedOrdinaryPeerNodes.add(node2);
        assignedOrdinaryPeerNodes.add(node3);
        Mockito.when(superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes())
                .thenReturn(assignedOrdinaryPeerNodes);

        Set<Node> superPeerNetworkNodes = new HashSet<>();
        superPeerNetworkNodes.add(fromNode);
        superPeerNetworkNodes.add(node4);
        superPeerNetworkNodes.add(node5);
        Mockito.when(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes())
                .thenReturn(superPeerNetworkNodes);
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(superPeerFloodingRoutingStrategy.getName());
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInOrdinaryPeerWithAssignedSuperPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 02 - Get forwarding nodes in ordinary peer " +
                "with assigned super peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(ordinaryPeerRoutingTable.getAssignedSuperPeer()).thenReturn(node1);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInOrdinaryPeerWithAssignedSuperPeerInStartingNode() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 03 - Get forwarding nodes in ordinary peer " +
                "with assigned super peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Mockito.when(ordinaryPeerRoutingTable.getAssignedSuperPeer()).thenReturn(node1);
        Mockito.when(node1.isActive()).thenReturn(true);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInOrdinaryPeerWithDeadAssignedSuperPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 04 - Get forwarding nodes in ordinary peer " +
                "with dead assigned super peer");

        Mockito.when(node1.isActive()).thenReturn(false);
        Message message = Mockito.mock(Message.class);
        Mockito.when(ordinaryPeerRoutingTable.getAssignedSuperPeer()).thenReturn(node1);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 4);
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 05 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer");

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 5);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeerInStartingNode() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 06 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 6);
        Assert.assertTrue(forwardingNodes.contains(fromNode));
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeerWithDeadNodes() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 07 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer with dead nodes");

        Mockito.when(node1.isActive()).thenReturn(false);
        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 4);
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 08 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1);
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2);

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeerInStartingNode() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 09 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1);
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2);

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeerWithDeadNodes() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 10 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer with dead nodes");

        Mockito.when(node1.isActive()).thenReturn(false);
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1);
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2);

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node2));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 11 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeerInStartingNode() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 12 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 3);
        Assert.assertTrue(forwardingNodes.contains(fromNode));
        Assert.assertTrue(forwardingNodes.contains(node4));
        Assert.assertTrue(forwardingNodes.contains(node5));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeerWithDeadSuperPeers() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 13 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer with dead super peers");

        Mockito.when(node4.isActive()).thenReturn(false);
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node5));
    }
}
