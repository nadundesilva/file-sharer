package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
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
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.strategy.SuperPeerFloodingRoutingStrategy
 * class.
 */
public class SuperPeerFloodingRoutingStrategyTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerFloodingRoutingStrategyTestCase.class);

    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private SuperPeerRoutingTable superPeerRoutingTable;
    private SuperPeerFloodingRoutingStrategy superPeerFloodingRoutingStrategy;
    private String queryResourceName;
    private Message message;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;
    private Node node5;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Super Peer Flooding Routing Strategy Test");

        serviceHolder.getConfiguration().setMaxUnstructuredPeerCount(100);
        serviceHolder.getConfiguration().setMaxSuperPeerCount(100);
        serviceHolder.getConfiguration().setMaxAssignedOrdinaryPeerCount(100);
        superPeerFloodingRoutingStrategy = new SuperPeerFloodingRoutingStrategy(serviceHolder);

        ordinaryPeerRoutingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));
        superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable(serviceHolder));
        queryResourceName = "Lord of the Rings";
        message = Message.parse("0047 SER 129.82.62.142 5070 0 0 \"" + queryResourceName +  "\"");

        fromNode = new Node("192.168.1.10", 6010);
        node1 = new Node("192.168.1.1", 6001);
        node2 = new Node("192.168.1.2", 6002);
        node3 = new Node("192.168.1.3", 6003);
        node4 = new Node("192.168.1.4", 6004);
        node5 = new Node("192.168.1.5", 6005);

        RoutingTable routingTable = serviceHolder.getRouter().getRoutingTable();
        routingTable.addUnstructuredNetworkRoutingTableEntry(fromNode.getIp(), fromNode.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node1.getIp(), node1.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node2.getIp(), node2.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node3.getIp(), node3.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node4.getIp(), node4.getPort());
        routingTable.addUnstructuredNetworkRoutingTableEntry(node5.getIp(), node5.getPort());

        Set<Node> unstructuredNetworkNode = new HashSet<>();
        unstructuredNetworkNode.add(fromNode);
        unstructuredNetworkNode.add(node1);
        unstructuredNetworkNode.add(node2);
        unstructuredNetworkNode.add(node3);
        unstructuredNetworkNode.add(node4);
        unstructuredNetworkNode.add(node5);
        unstructuredNetworkNode.forEach(node -> {
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node.getIp(), node.getPort());
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
        superPeerNetworkNodes.add(fromNode);
        superPeerNetworkNodes.add(node4);
        superPeerNetworkNodes.add(node5);
        superPeerNetworkNodes.forEach(node -> {
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(node.getIp(), node.getPort());
        });
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

        ordinaryPeerRoutingTable.setAssignedSuperPeer(node1.getIp(), node1.getPort());
        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesInOrdinaryPeerWithDeadAssignedSuperPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 04 - Get forwarding nodes in ordinary peer " +
                "with dead assigned super peer");

        ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

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

        ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

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

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

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

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

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

        serviceHolder.getRouter().getRoutingTable().get(node1.getIp(), node1.getPort()).setState(NodeState.INACTIVE);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addAggregatedResource(queryResourceName, node1.getIp(), node1.getPort());
        superPeerResourceIndex.addAggregatedResource(queryResourceName, node2.getIp(), node2.getPort());

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node2));
    }

    @Test(priority = 2)
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeer() {
        logger.info("Running Super Peer Flooding Routing Strategy Test 11 - Get forwarding nodes in super peer " +
                "with resource not in assigned ordinary peer");

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

        superPeerRoutingTable.get(node4.getIp(), node4.getPort()).setState(NodeState.INACTIVE);
        serviceHolder.getRouter().getRoutingTable().get(node4.getIp(), node4.getPort()).setState(NodeState.INACTIVE);

        serviceHolder.promoteToSuperPeer();
        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerFloodingRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node5));
    }
}
