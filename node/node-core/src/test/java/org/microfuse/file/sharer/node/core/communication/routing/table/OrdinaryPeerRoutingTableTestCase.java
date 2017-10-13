package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.OrdinaryPeerRoutingTable class.
 */
public class OrdinaryPeerRoutingTableTestCase extends BaseTestCase {
    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node assignedSuperPeer;

    @BeforeMethod
    public void initializeMethod() {
        ordinaryPeerRoutingTable = new OrdinaryPeerRoutingTable();

        node1 = new Node();
        node1.setIp("192.168.1.1");
        node1.setPort(4532);
        node1.setAlive(true);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node1);

        node2 = new Node();
        node2.setIp("192.168.1.2");
        node2.setPort(6542);
        node2.setAlive(true);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node2);

        node3 = new Node();
        node3.setIp("192.168.1.3");
        node3.setPort(5643);
        node3.setAlive(false);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node3);

        assignedSuperPeer = new Node();
        assignedSuperPeer.setIp("192.168.1.3");
        assignedSuperPeer.setPort(5643);
        assignedSuperPeer.setAlive(false);
        ordinaryPeerRoutingTable.setAssignedSuperPeer(assignedSuperPeer);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(assignedSuperPeer);
    }

    @Test
    public void testGetUnstructuredNetworkRoutingTableNode() {
        Node unstructuredNetworkRoutingTableNode =
                ordinaryPeerRoutingTable.getUnstructuredNetworkRoutingTableNode("192.168.1.2", 6542);

        Assert.assertNotNull(unstructuredNetworkRoutingTableNode);
        Assert.assertEquals(unstructuredNetworkRoutingTableNode.getIp(), "192.168.1.2");
        Assert.assertEquals(unstructuredNetworkRoutingTableNode.getPort(), 6542);
        Assert.assertEquals(unstructuredNetworkRoutingTableNode.isAlive(), true);
    }

    @Test
    public void testGetUnstructuredNetworkRoutingTableNonExistentNode() {
        Node unstructuredNetworkRoutingTableNode =
                ordinaryPeerRoutingTable.getUnstructuredNetworkRoutingTableNode("192.168.1.4", 7525);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test
    public void testGetAllAssignedOrdinaryNetworkRoutingTableNodesCopying() {
        Set<Node> nodes = ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test
    public void testRemoveFromAllUnstructuredNetworkNode() {
        Assert.assertTrue(ordinaryPeerRoutingTable.removeFromAll(node1));

        Object internalStateUnstructuredNetwork =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetwork);
        Assert.assertTrue(internalStateUnstructuredNetwork instanceof Set<?>);
        Set<?> unstructuredNetwork = (Set<?>) internalStateUnstructuredNetwork;
        Assert.assertFalse(unstructuredNetwork.contains(node1));
    }

    @Test
    public void testRemoveFromAllAssignedSuperPeerNode() {
        Assert.assertTrue(ordinaryPeerRoutingTable.removeFromAll(assignedSuperPeer));

        Object internalStateUnstructuredNetwork =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetwork);
        Assert.assertTrue(internalStateUnstructuredNetwork instanceof Set<?>);
        Set<?> unstructuredNetwork = (Set<?>) internalStateUnstructuredNetwork;
        Assert.assertFalse(unstructuredNetwork.contains(assignedSuperPeer));

        Object internalStateAssignedSuperPeer =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "assignedSuperPeer");
        Assert.assertNull(internalStateAssignedSuperPeer);
    }

    @Test
    public void testGetAll() {
        Set<Node> nodes = ordinaryPeerRoutingTable.getAll();

        Assert.assertEquals(nodes.size(), 3);
        Assert.assertTrue(nodes.contains(node1));
        Assert.assertTrue(nodes.contains(node2));
        Assert.assertTrue(nodes.contains(node3));
    }

    @Test
    public void getGetUnstructuredNetworkNode() {
        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setAlive(true);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        Node foundNode = ordinaryPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(foundNode);
        Assert.assertTrue(foundNode == node);
    }

    @Test
    public void getGetAssignedSuperPeerNode() {
        Node node = new Node();
        node.setIp("192.168.1.100");
        node.setPort(5824);
        node.setAlive(true);
        ordinaryPeerRoutingTable.setAssignedSuperPeer(node);

        Node foundNode = ordinaryPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(foundNode);
        Assert.assertTrue(foundNode == node);
    }

    @Test
    public void getNonExistentNode() {
        Node node = ordinaryPeerRoutingTable.get("192.168.1.243", 7846);

        Assert.assertNull(node);
    }

    @Test
    public void testClear() {
        ordinaryPeerRoutingTable.clear();

        Object internalStateUnstructuredNetworkNodes =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetworkNodes);
        Assert.assertTrue(internalStateUnstructuredNetworkNodes instanceof Set<?>);
        Set<?> unstructuredNetworkNodes = (Set<?>) internalStateUnstructuredNetworkNodes;
        Assert.assertEquals(unstructuredNetworkNodes.size(), 0);

        Object internalStateAssignedSuperPeer =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "assignedSuperPeer");
        Assert.assertNull(internalStateAssignedSuperPeer);
    }
}