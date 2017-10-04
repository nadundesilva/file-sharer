package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.SuperPeerRoutingTable class.
 *
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class SuperPeerRoutingTableTestCase extends BaseTestCase {
    private SuperPeerRoutingTable superPeerRoutingTable;
    private Node ordinaryPeerNode1;
    private Node superPeerNode1;

    @BeforeMethod
    public void initializeMethod() {
        superPeerRoutingTable = new SuperPeerRoutingTable();

        ordinaryPeerNode1 = new Node();
        ordinaryPeerNode1.setIp("192.168.1.1");
        ordinaryPeerNode1.setPort(4532);
        ordinaryPeerNode1.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode1);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(ordinaryPeerNode1);

        Node ordinaryPeerNode2 = new Node();
        ordinaryPeerNode2.setIp("192.168.1.2");
        ordinaryPeerNode2.setPort(6542);
        ordinaryPeerNode2.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(ordinaryPeerNode2);
        superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(ordinaryPeerNode2);

        Node ordinaryPeerNode3 = new Node();
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

        Node superPeerNode2 = new Node();
        superPeerNode2.setIp("192.168.1.5");
        superPeerNode2.setPort(7431);
        superPeerNode2.setAlive(true);
        superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(superPeerNode2);
        superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(superPeerNode2);

        Node superPeerNode3 = new Node();
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
}
