package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.SuperPeerRoutingTable class.
 */
public class SuperPeerRoutingTableTestCase extends BaseTestCase {
    private SuperPeerRoutingTable superPeerRoutingTable;
    private Node ordinaryPeerNode1;
    private Node ordinaryPeerNode2;
    private Node ordinaryPeerNode3;
    private Node superPeerNode1;
    private Node superPeerNode2;
    private Node superPeerNode3;

    @BeforeMethod
    public void initializeMethod() {
        superPeerRoutingTable = new SuperPeerRoutingTable();

        ordinaryPeerNode1 = new Node();
        ordinaryPeerNode1.setIp("192.168.1.1");
        ordinaryPeerNode1.setPort(4532);
        ordinaryPeerNode1.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode1);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(ordinaryPeerNode1);

        ordinaryPeerNode2 = new Node();
        ordinaryPeerNode2.setIp("192.168.1.2");
        ordinaryPeerNode2.setPort(6542);
        ordinaryPeerNode2.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode2);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(ordinaryPeerNode2);

        ordinaryPeerNode3 = new Node();
        ordinaryPeerNode3.setIp("192.168.1.3");
        ordinaryPeerNode3.setPort(5643);
        ordinaryPeerNode3.setAlive(false);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode3);

        superPeerNode1 = new Node();
        superPeerNode1.setIp("192.168.1.4");
        superPeerNode1.setPort(7543);
        superPeerNode1.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode1);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(superPeerNode1);

        superPeerNode2 = new Node();
        superPeerNode2.setIp("192.168.1.5");
        superPeerNode2.setPort(7431);
        superPeerNode2.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode2);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(superPeerNode2);

        superPeerNode3 = new Node();
        superPeerNode3.setIp("192.168.1.6");
        superPeerNode3.setPort(4562);
        superPeerNode3.setAlive(false);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode3);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(superPeerNode3);
    }

    @Test
    public void testGetSuperPeerNetworkRoutingTableNode() {
        Node superPeerNetworkRoutingTableNode =
                superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode("192.168.1.5", 7431);

        Assert.assertNotNull(superPeerNetworkRoutingTableNode);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getIp(), "192.168.1.5");
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getPort(), 7431);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.isAlive(), true);
    }

    @Test
    public void testGetSuperPeerNetworkRoutingTableNonExistentNode() {
        Node unstructuredNetworkRoutingTableNode =
                superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode("192.168.1.2", 6542);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test
    public void testGetAssignedOrdinaryNetworkRoutingTableNode() {
        Node superPeerNetworkRoutingTableNode =
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode("192.168.1.2", 6542);

        Assert.assertNotNull(superPeerNetworkRoutingTableNode);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getIp(), "192.168.1.2");
        Assert.assertEquals(superPeerNetworkRoutingTableNode.getPort(), 6542);
        Assert.assertEquals(superPeerNetworkRoutingTableNode.isAlive(), true);
    }

    @Test
    public void testGetAssignedOrdinaryNetworkRoutingTableNonExistentNode() {
        Node unstructuredNetworkRoutingTableNode =
                superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode("192.168.1.5", 7431);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test
    public void testGetAllAssignedOrdinaryNetworkRoutingTableNodesCopying() {
        Set<Node> nodes = superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(superPeerRoutingTable, "assignedOrdinaryPeerNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test
    public void testGetAllSuperPeerNetworkRoutingTableNodes() {
        Set<Node> nodes = superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(superPeerRoutingTable, "superPeerNetworkNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test
    public void testRemoveFromAllASuperPeerNode() {
        Assert.assertTrue(superPeerRoutingTable.removeFromAll(superPeerNode1));

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

    @Test
    public void testRemoveFromAllAOrdinaryPeerNode() {
        Assert.assertTrue(superPeerRoutingTable.removeFromAll(ordinaryPeerNode1));

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

    @Test
    public void testGetAll() {
        Set<Node> nodes = superPeerRoutingTable.getAll();

        Assert.assertEquals(nodes.size(), 6);
        Assert.assertTrue(nodes.contains(ordinaryPeerNode1));
        Assert.assertTrue(nodes.contains(ordinaryPeerNode2));
        Assert.assertTrue(nodes.contains(ordinaryPeerNode3));
        Assert.assertTrue(nodes.contains(superPeerNode1));
        Assert.assertTrue(nodes.contains(superPeerNode2));
        Assert.assertTrue(nodes.contains(superPeerNode3));
    }

    @Test
    public void getGetUnstructuredNetworkNode() {
        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        Node foundNode = superPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(node);
        Assert.assertTrue(foundNode == node);
    }

    @Test
    public void getGetAssignedOrdinaryPeersNode() {
        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setAlive(true);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(node);

        Node foundNode = superPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(foundNode);
        Assert.assertTrue(foundNode == node);
    }

    @Test
    public void getGetSuperPeerNetworkNode() {
        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setAlive(true);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(node);

        Node foundNode = superPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(foundNode);
        Assert.assertTrue(foundNode == node);
    }

    @Test
    public void getNonExistentNode() {
        Node node = superPeerRoutingTable.get("192.168.1.243", 7846);

        Assert.assertNull(node);
    }

    @Test
    public void testClear() {
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

    @Test
    public void testAddAssignedSuperPeerNetworkRoutingTableNodeAfterMaxThreshold() {
        ServiceHolder.getConfiguration().setMaxAssignedOrdinaryPeerCount(3);

        Node newNode1 = Mockito.mock(Node.class);
        Node newNode2 = Mockito.mock(Node.class);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(newNode1);
        boolean isSuccessful = superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(newNode2);
        Set<Node> assignedSuperPeer = superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes();

        Assert.assertFalse(isSuccessful);
        Assert.assertEquals(assignedSuperPeer.size(), 3);
        Assert.assertTrue(assignedSuperPeer.contains(ordinaryPeerNode1));
        Assert.assertTrue(assignedSuperPeer.contains(ordinaryPeerNode2));
        Assert.assertTrue(assignedSuperPeer.contains(newNode1));
    }
}