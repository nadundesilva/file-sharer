package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
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
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.strategy.SuperPeerRandomWalkRoutingStrategy
 * class.
 */
public class SuperPeerRandomWalkRoutingStrategyTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRandomWalkRoutingStrategyTestCase.class);

    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private SuperPeerRoutingTable superPeerRoutingTable;
    private SuperPeerRandomWalkRoutingStrategy superPeerRandomWalkRoutingStrategy;
    private String queryResourceName;
    private Node fromNode;
    private Node fromSuperPeerNode;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;
    private Node node5;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Super Peer Random Walk Routing Strategy Test");

        superPeerRandomWalkRoutingStrategy = new SuperPeerRandomWalkRoutingStrategy(serviceHolder);

        ordinaryPeerRoutingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));
        superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable(serviceHolder));
        queryResourceName = "Lord of the Rings";

        fromNode = new Node("192.168.1.10", 6010);
        fromSuperPeerNode = new Node("192.168.1.11", 6011);
        node1 = new Node("192.168.1.1", 6001);
        node2 = new Node("192.168.1.2", 6002);
        node3 = new Node("192.168.1.3", 6003);
        node4 = new Node("192.168.1.4", 6004);
        node5 = new Node("192.168.1.5", 6005);

        Set<Node> unstructuredNetworkNode = new HashSet<>();
        unstructuredNetworkNode.add(fromSuperPeerNode);
        unstructuredNetworkNode.add(fromNode);
        unstructuredNetworkNode.add(node1);
        unstructuredNetworkNode.add(node2);
        unstructuredNetworkNode.add(node3);
        unstructuredNetworkNode.add(node4);
        unstructuredNetworkNode.add(node5);
        unstructuredNetworkNode.forEach(node -> {
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
            serviceHolder.getRouter().getRoutingTable()
                    .addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
        });

        Set<Node> assignedOrdinaryPeerNodes = new HashSet<>();
        assignedOrdinaryPeerNodes.add(fromNode);
        assignedOrdinaryPeerNodes.add(node1);
        assignedOrdinaryPeerNodes.add(node2);
        assignedOrdinaryPeerNodes.add(node3);
        assignedOrdinaryPeerNodes.forEach(node -> {
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(node.getIp(), node.getPort());
        });

        Set<Node> superPeerNetworkNodes = new HashSet<>();
        superPeerNetworkNodes.add(fromSuperPeerNode);
        superPeerNetworkNodes.add(node4);
        superPeerNetworkNodes.add(node5);
        superPeerNetworkNodes.forEach(node -> {
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(node.getIp(), node.getPort());
        });
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(superPeerRandomWalkRoutingStrategy.getName());
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInOrdinaryPeerWithAssignedSuperPeer() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 02 - Get forwarding nodes in ordinary peer " +
                "with assigned super peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(ordinaryPeerRoutingTable.getAssignedSuperPeer()).thenReturn(node1);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInOrdinaryPeerWithAssignedSuperPeerInStartingNode() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 03 - Get forwarding nodes in ordinary peer " +
                "with assigned super peer in the starting node");

        Message message = Mockito.mock(Message.class);
        ordinaryPeerRoutingTable.setAssignedSuperPeer(node1.getIp(), node1.getPort());
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInOrdinaryPeerWithDeadAssignedSuperPeer() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 04 - Get forwarding nodes in ordinary peer " +
                "with dead assigned super peer");

        ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromSuperPeerNode) || forwardingNodes.contains(node2) ||
                forwardingNodes.contains(node3) || forwardingNodes.contains(node4) ||
                forwardingNodes.contains(node5));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeer() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 05 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer");

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromSuperPeerNode) || forwardingNodes.contains(node1) ||
                forwardingNodes.contains(node2) || forwardingNodes.contains(node3) ||
                forwardingNodes.contains(node4) || forwardingNodes.contains(node5)
        );
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeerInStartingNode() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 06 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromSuperPeerNode) || forwardingNodes.contains(fromNode) ||
                forwardingNodes.contains(node1) || forwardingNodes.contains(node2) ||
                forwardingNodes.contains(node3) || forwardingNodes.contains(node4) ||
                forwardingNodes.contains(node5)
        );
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeerWithDeadNodes() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 07 - Get forwarding nodes in ordinary peer " +
                "with unassigned super peer with dead nodes");

        ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromSuperPeerNode) || forwardingNodes.contains(fromNode) ||
                forwardingNodes.contains(node2) || forwardingNodes.contains(node3) ||
                forwardingNodes.contains(node4) || forwardingNodes.contains(node5)
        );
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeer() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 08 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1) || forwardingNodes.contains(node2));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeerInStartingNode() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 09 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1) || forwardingNodes.contains(node2));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeerWithDeadNodes() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 10 - Get forwarding nodes in super peer " +
                "with resource in assigned ordinary peer with dead nodes");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        superPeerRoutingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node2));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeer() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 11 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node4) || forwardingNodes.contains(node5));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeerInStartingNode() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 12 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer in the starting node");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromSuperPeerNode) || forwardingNodes.contains(node4) ||
                forwardingNodes.contains(node5));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeerWithDeadNodes() {
        logger.info("Running Super Peer Random Walk Routing Strategy Test 13 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer with dead nodes");

        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        superPeerRoutingTable.get(node4.getIp(), node4.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node4.getIp(), node4.getPort()).setState(NodeState.INACTIVE);

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node5));
    }
}
