package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.NodeState;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.SuperPeerRoutingTable class.
 */
public class SuperPeerRoutingTableTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(SuperPeerRoutingTableTestCase.class);

    private SuperPeerRoutingTable superPeerRoutingTable;
    private Node ordinaryPeerNode1;
    private Node ordinaryPeerNode2;
    private Node ordinaryPeerNode3;
    private Node superPeerNode1;
    private Node superPeerNode2;
    private Node superPeerNode3;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Super Peer Routing Table Test");

        superPeerRoutingTable = new SuperPeerRoutingTable(serviceHolder);

        ordinaryPeerNode1 = new Node();
        ordinaryPeerNode1.setIp("192.168.1.1");
        ordinaryPeerNode1.setPort(4532);
        ordinaryPeerNode1.setState(NodeState.ACTIVE);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode1);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                ordinaryPeerNode1.getIp(), ordinaryPeerNode1.getPort());

        ordinaryPeerNode2 = new Node();
        ordinaryPeerNode2.setIp("192.168.1.2");
        ordinaryPeerNode2.setPort(6542);
        ordinaryPeerNode2.setState(NodeState.ACTIVE);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode2);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                ordinaryPeerNode2.getIp(), ordinaryPeerNode2.getPort());

        ordinaryPeerNode3 = new Node();
        ordinaryPeerNode3.setIp("192.168.1.3");
        ordinaryPeerNode3.setPort(5643);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode3);
        superPeerRoutingTable.get(ordinaryPeerNode3.getIp(), ordinaryPeerNode3.getPort()).setState(NodeState.INACTIVE);

        superPeerNode1 = new Node();
        superPeerNode1.setIp("192.168.1.4");
        superPeerNode1.setPort(7543);
        superPeerNode1.setState(NodeState.ACTIVE);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode1);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                superPeerNode1.getIp(), superPeerNode1.getPort());

        superPeerNode2 = new Node();
        superPeerNode2.setIp("192.168.1.5");
        superPeerNode2.setPort(7431);
        superPeerNode2.setState(NodeState.ACTIVE);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode2);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                superPeerNode2.getIp(), superPeerNode2.getPort());

        superPeerNode3 = new Node();
        superPeerNode3.setIp("192.168.1.6");
        superPeerNode3.setPort(4562);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode3);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(
                superPeerNode3.getIp(), superPeerNode3.getPort());
        superPeerRoutingTable.get(superPeerNode3.getIp(), superPeerNode3.getPort()).setState(NodeState.INACTIVE);
    }

    @Test(priority = 1)
    public void testGetSuperPeerNetworkRoutingTableNode() {
        logger.info("Running Super Peer Routing Table Test 01 - Get name");

        Node superPeerNetworkRoutingTableNode =
                superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode("192.168.1.5", 7431);

        Assert.assertNotNull(superPeerNetworkRoutingTableNode);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getIp(), "192.168.1.5");
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getPort(), 7431);
        Assert.assertTrue(superPeerNetworkRoutingTableNode.isActive());
    }

    @Test(priority = 2)
    public void testGetSuperPeerNetworkRoutingTableNonExistentNode() {
        logger.info("Running Super Peer Routing Table Test 02 - Get super peer network routing table " +
                "non existent node");

        Node unstructuredNetworkRoutingTableNode =
                superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode("192.168.1.2", 6542);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test(priority = 1)
    public void testGetAssignedOrdinaryNetworkRoutingTableNode() {
        logger.info("Running Super Peer Routing Table Test 03 - Get assigned ordinary network routing table node");

        Node superPeerNetworkRoutingTableNode =
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode("192.168.1.2", 6542);

        Assert.assertNotNull(superPeerNetworkRoutingTableNode);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getIp(), "192.168.1.2");
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getPort(), 6542);
        Assert.assertTrue(superPeerNetworkRoutingTableNode.isActive());
    }

    @Test(priority = 2)
    public void testGetAssignedOrdinaryNetworkRoutingTableNonExistentNode() {
        logger.info("Running Super Peer Routing Table Test 04 - Get assigned ordinary network routing table " +
                "non existent node");

        Node unstructuredNetworkRoutingTableNode =
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode("192.168.1.5", 7431);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test(priority = 1)
    public void testGetAllAssignedOrdinaryNetworkRoutingTableNodesCopying() {
        logger.info("Running Super Peer Routing Table Test 05 - Get all assigned ordinary network routing table " +
                "nodes copying");

        Set<Node> nodes = superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(superPeerRoutingTable, "assignedOrdinaryPeerNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test(priority = 1)
    public void testGetAllSuperPeerNetworkRoutingTableNodes() {
        logger.info("Running Super Peer Routing Table Test 06 - Get all super peer network routing table nodes");

        Set<Node> nodes = superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(superPeerRoutingTable, "superPeerNetworkNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test(priority = 1)
    public void testRemoveFromAllASuperPeerNode() {
        logger.info("Running Super Peer Routing Table Test 07 - Remove from all super peer node");

        Assert.assertTrue(superPeerRoutingTable.removeFromAll(superPeerNode1.getIp(), superPeerNode1.getPort()));

        Object internalStateUnstructuredNetwork =
                Whitebox.getInternalState(superPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetwork);
        Assert.assertTrue(internalStateUnstructuredNetwork instanceof Set<?>);
        Set<?> unstructuredNetwork = (Set<?>) internalStateUnstructuredNetwork;
        Assert.assertFalse(unstructuredNetwork.contains(superPeerNode1));

        Object internalStateSuperPeerNetwork =
                Whitebox.getInternalState(superPeerRoutingTable, "superPeerNetworkNodes");
        Assert.assertNotNull(internalStateSuperPeerNetwork);
        Assert.assertTrue(internalStateSuperPeerNetwork instanceof Set<?>);
        Set<?> superPeerNetwork = (Set<?>) internalStateSuperPeerNetwork;
        Assert.assertFalse(superPeerNetwork.contains(superPeerNode1));

        Object internalStateAssignedOrdinaryPeerNodes =
                Whitebox.getInternalState(superPeerRoutingTable, "assignedOrdinaryPeerNodes");
        Assert.assertNotNull(internalStateAssignedOrdinaryPeerNodes);
        Assert.assertTrue(internalStateAssignedOrdinaryPeerNodes instanceof Set<?>);
        Set<?> assignedOrdinaryPeerNodes = (Set<?>) internalStateAssignedOrdinaryPeerNodes;
        Assert.assertFalse(assignedOrdinaryPeerNodes.contains(superPeerNode1));
    }

    @Test(priority = 1)
    public void testRemoveFromAllAssignedAOrdinaryPeerNode() {
        logger.info("Running Super Peer Routing Table Test 08 - Remove from all assigned ordinary peer peer node");

        Assert.assertTrue(superPeerRoutingTable.removeFromAll(
                ordinaryPeerNode1.getIp(), ordinaryPeerNode1.getPort()));

        Object internalStateUnstructuredNetwork =
                Whitebox.getInternalState(superPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetwork);
        Assert.assertTrue(internalStateUnstructuredNetwork instanceof Set<?>);
        Set<?> unstructuredNetwork = (Set<?>) internalStateUnstructuredNetwork;
        Assert.assertFalse(unstructuredNetwork.contains(ordinaryPeerNode1));

        Object internalStateAssignedOrdinaryPeerNodes =
                Whitebox.getInternalState(superPeerRoutingTable, "assignedOrdinaryPeerNodes");
        Assert.assertNotNull(internalStateAssignedOrdinaryPeerNodes);
        Assert.assertTrue(internalStateAssignedOrdinaryPeerNodes instanceof Set<?>);
        Set<?> assignedOrdinaryPeerNodes = (Set<?>) internalStateAssignedOrdinaryPeerNodes;
        Assert.assertFalse(assignedOrdinaryPeerNodes.contains(ordinaryPeerNode1));
    }

    @Test(priority = 1)
    public void testGetAll() {
        logger.info("Running Super Peer Routing Table Test 09 - Get all");

        Set<Node> nodes = superPeerRoutingTable.getAll();

        Assert.assertEquals(nodes.size(), 6);
        Assert.assertTrue(nodes.contains(ordinaryPeerNode1));
        Assert.assertTrue(nodes.contains(ordinaryPeerNode2));
        Assert.assertTrue(nodes.contains(ordinaryPeerNode3));
        Assert.assertTrue(nodes.contains(superPeerNode1));
        Assert.assertTrue(nodes.contains(superPeerNode2));
        Assert.assertTrue(nodes.contains(superPeerNode3));
    }

    @Test(priority = 1)
    public void testGetUnstructuredNetworkNode() {
        logger.info("Running Super Peer Routing Table Test 10 - Get unstructured network node");

        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setState(NodeState.ACTIVE);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        Node foundNode = superPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(node);
        Assert.assertTrue(foundNode == node);
    }

    @Test(priority = 2)
    public void getGetAssignedOrdinaryPeersNode() {
        logger.info("Running Super Peer Routing Table Test 11 - Get assigned ordinary peer node");

        String ip = "192.168.1.100";
        int port = 5824;

        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(ip, port);
        Node foundNode = superPeerRoutingTable.get(ip, port);

        Assert.assertNotNull(foundNode);
    }

    @Test(priority = 3)
    public void getGetSuperPeerNetworkNode() {
        logger.info("Running Super Peer Routing Table Test 12 - Get super peer network node");

        String ip = "192.168.1.100";
        int port = 5824;

        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(ip, port);
        Node foundNode = superPeerRoutingTable.get(ip, port);

        Assert.assertNotNull(foundNode);
    }

    @Test(priority = 4)
    public void getNonExistentNode() {
        logger.info("Running Super Peer Routing Table Test 13 - Get non existent node");

        Node node = superPeerRoutingTable.get("192.168.1.243", 7846);

        Assert.assertNull(node);
    }

    @Test(priority = 1)
    public void testClear() {
        logger.info("Running Super Peer Routing Table Test 14 - Clear");

        superPeerRoutingTable.clear();

        Object internalStateSuperPeerNetworkNodes =
                Whitebox.getInternalState(superPeerRoutingTable, "superPeerNetworkNodes");
        Assert.assertNotNull(internalStateSuperPeerNetworkNodes);
        Assert.assertTrue(internalStateSuperPeerNetworkNodes instanceof Set<?>);
        Set<?> superPeerNetworkNodes = (Set<?>) internalStateSuperPeerNetworkNodes;
        Assert.assertEquals(superPeerNetworkNodes.size(), 0);


        Object internalStateAssignedOrdinaryPeerNodes =
                Whitebox.getInternalState(superPeerRoutingTable, "assignedOrdinaryPeerNodes");
        Assert.assertNotNull(internalStateAssignedOrdinaryPeerNodes);
        Assert.assertTrue(internalStateAssignedOrdinaryPeerNodes instanceof Set<?>);
        Set<?> assignedOrdinaryPeerNodes = (Set<?>) internalStateSuperPeerNetworkNodes;
        Assert.assertEquals(assignedOrdinaryPeerNodes.size(), 0);
    }

    @Test(priority = 2)
    public void testAddAssignedSuperPeerNetworkRoutingTableNodeAfterMaxThreshold() {
        logger.info("Running Super Peer Routing Table Test 15 - Add assigned super peer network routing table " +
                "node after max threshold");

        serviceHolder.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);

        Node newNode1 = new Node("192.168.1.2", 7854);
        Node newNode2 = new Node("192.168.1.3", 8456);

        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(newNode1.getIp(), newNode1.getPort());
        boolean isSuccessful = superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(
                newNode2.getIp(), newNode2.getPort());
        Set<Node> assignedSuperPeer = superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes();

        Assert.assertFalse(isSuccessful);
        Assert.assertEquals(assignedSuperPeer.size(), 3);
        Assert.assertTrue(assignedSuperPeer.contains(ordinaryPeerNode1));
        Assert.assertTrue(assignedSuperPeer.contains(ordinaryPeerNode2));
        Assert.assertTrue(assignedSuperPeer.contains(newNode1));
        Assert.assertFalse(assignedSuperPeer.contains(newNode2));
    }
}
