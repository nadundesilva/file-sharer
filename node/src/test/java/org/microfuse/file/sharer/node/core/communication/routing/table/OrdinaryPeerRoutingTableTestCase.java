package org.microfuse.file.sharer.node.core.communication.routing.table;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.OrdinaryPeerRoutingTable class.
 *
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class OrdinaryPeerRoutingTableTestCase extends BaseTestCase {
    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private Node node1;

    @BeforeMethod
    public void initializeMethod() {
        ordinaryPeerRoutingTable = new OrdinaryPeerRoutingTable();

        node1 = new Node();
        node1.setIp("192.168.1.1");
        node1.setPort(4532);
        node1.setAlive(true);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node1);

        Node node2 = new Node();
        node2.setIp("192.168.1.2");
        node2.setPort(6542);
        node2.setAlive(true);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node2);

        Node node3 = new Node();
        node3.setIp("192.168.1.3");
        node3.setPort(5643);
        node3.setAlive(false);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node3);
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
}