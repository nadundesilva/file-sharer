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
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.OrdinaryPeerRoutingTable class.
 */
public class OrdinaryPeerRoutingTableTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(OrdinaryPeerRoutingTableTestCase.class);

    private OrdinaryPeerRoutingTable ordinaryPeerRoutingTable;
    private Node node1;
    private Node node2;
    private Node node3;
    private Node assignedSuperPeer;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Ordinary Peer Routing Table Test");

        ordinaryPeerRoutingTable = new OrdinaryPeerRoutingTable(serviceHolder);

        node1 = new Node("192.168.1.1", 4532);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node1);

        node2 = new Node("192.168.1.2", 6542);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node2);

        node3 = new Node("192.168.1.3", 5643);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node3.getIp(), node3.getPort());

        assignedSuperPeer = new Node("192.168.1.4", 5644);
        ordinaryPeerRoutingTable.setAssignedSuperPeer(assignedSuperPeer.getIp(), assignedSuperPeer.getPort());
        ordinaryPeerRoutingTable
                .addUnstructuredNetworkRoutingTableEntry(assignedSuperPeer.getIp(), assignedSuperPeer.getPort());
    }

    @Test(priority = 1)
    public void testGetUnstructuredNetworkRoutingTableNode() {
        logger.info("Running Ordinary Peer Routing Table Test 01 - Get unstructured network routing table node");

        Node unstructuredNetworkRoutingTableNode =
                ordinaryPeerRoutingTable.getUnstructuredNetworkRoutingTableNode(node2.getIp(), node2.getPort());

        Assert.assertNotNull(unstructuredNetworkRoutingTableNode);
        Assert.assertEquals(unstructuredNetworkRoutingTableNode.getIp(), node2.getIp());
        Assert.assertEquals(unstructuredNetworkRoutingTableNode.getPort(), node2.getPort());
        Assert.assertTrue(unstructuredNetworkRoutingTableNode.isActive());
    }

    @Test(priority = 2)
    public void testGetUnstructuredNetworkRoutingTableNonExistentNode() {
        logger.info("Running Ordinary Peer Routing Table Test 02 - Get unstructured network routing table " +
                "non existent node");

        Node unstructuredNetworkRoutingTableNode =
                ordinaryPeerRoutingTable.getUnstructuredNetworkRoutingTableNode("192.168.1.4", 7525);

        Assert.assertNull(unstructuredNetworkRoutingTableNode);
    }

    @Test(priority = 2)
    public void testGetAllAssignedOrdinaryNetworkRoutingTableNodesCopying() {
        logger.info("Running Ordinary Peer Routing Table Test 03 - Get all assigned ordinary network routing table " +
                "nodes copying");

        Set<Node> nodes = ordinaryPeerRoutingTable.getAllUnstructuredNetworkRoutingTableNodes();
        Object internalState = Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertFalse(nodes == internalState);
    }

    @Test(priority = 1)
    public void testRemoveFromAllUnstructuredNetworkNode() {
        logger.info("Running Ordinary Peer Routing Table Test 04 - Remove from all unstructured network node");

        Assert.assertTrue(ordinaryPeerRoutingTable.removeFromAll(node1.getIp(), node1.getPort()));

        Object internalStateUnstructuredNetwork =
                Whitebox.getInternalState(ordinaryPeerRoutingTable, "unstructuredNetworkNodes");
        Assert.assertNotNull(internalStateUnstructuredNetwork);
        Assert.assertTrue(internalStateUnstructuredNetwork instanceof Set<?>);
        Set<?> unstructuredNetwork = (Set<?>) internalStateUnstructuredNetwork;
        Assert.assertFalse(unstructuredNetwork.contains(node1));
    }

    @Test(priority = 1)
    public void testRemoveFromAllAssignedSuperPeerNode() {
        logger.info("Running Ordinary Peer Routing Table Test 05 - Remove from all assigned super peer node");

        Assert.assertTrue(ordinaryPeerRoutingTable.removeFromAll(
                assignedSuperPeer.getIp(), assignedSuperPeer.getPort()));

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

    @Test(priority = 1)
    public void testGetAll() {
        logger.info("Running Ordinary Peer Routing Table Test 06 - Get all");

        Set<Node> nodes = ordinaryPeerRoutingTable.getAll();

        Assert.assertEquals(nodes.size(), 4);
        Assert.assertTrue(nodes.contains(assignedSuperPeer));
        Assert.assertTrue(nodes.contains(node1));
        Assert.assertTrue(nodes.contains(node2));
        Assert.assertTrue(nodes.contains(node3));
    }

    @Test(priority = 1)
    public void testGetUnstructuredNetworkNode() {
        logger.info("Running Ordinary Peer Routing Table Test 07 - Get unstructured network node");

        Node node = new Node("192.168.1.100", 5824);
        ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(node);

        Node foundNode = ordinaryPeerRoutingTable.get(node.getIp(), node.getPort());

        Assert.assertNotNull(foundNode);
        Assert.assertTrue(foundNode == node);
    }

    @Test(priority = 2)
    public void testGetAssignedSuperPeerNode() {
        logger.info("Running Ordinary Peer Routing Table Test 08 - Get assigned super peer node");

        String ip = "192.168.1.100";
        int port = 5824;

        ordinaryPeerRoutingTable.setAssignedSuperPeer(ip, port);
        Node foundNode = ordinaryPeerRoutingTable.get(ip, port);

        Assert.assertNotNull(foundNode);
    }

    @Test(priority = 3)
    public void testGetNonExistentNode() {
        logger.info("Running Ordinary Peer Routing Table Test 09 - Get non existent node");

        Node node = ordinaryPeerRoutingTable.get("192.168.1.243", 7846);

        Assert.assertNull(node);
    }

    @Test(priority = 1)
    public void testClear() {
        logger.info("Running Ordinary Peer Routing Table Test 10 - Clear");

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

    @Test(priority = 4)
    public void testGarbageCollectionWithInactiveUnstructuredNetworkNode() {
        logger.info("Running Ordinary Peer Routing Table Test 11 - " +
                "Garbage collection with inactive unstructured network node");

        Node node = ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort());
        node.setState(NodeState.INACTIVE);
        ordinaryPeerRoutingTable.collectGarbage();

        Assert.assertNotNull(node);
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(assignedSuperPeer.getIp(), assignedSuperPeer.getPort()));
        Assert.assertNull(ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()));
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(node2.getIp(), node2.getPort()));
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(node3.getIp(), node3.getPort()));
    }

    @Test(priority = 4)
    public void testGarbageCollectionWithInactiveAssignedSuperPeer() {
        logger.info("Running Ordinary Peer Routing Table Test 12 - " +
                "Garbage collection with inactive assigned super peer");

        Node node = ordinaryPeerRoutingTable.get(assignedSuperPeer.getIp(), assignedSuperPeer.getPort());
        node.setState(NodeState.INACTIVE);
        ordinaryPeerRoutingTable.collectGarbage();

        Assert.assertNotNull(node);
        Assert.assertNull(ordinaryPeerRoutingTable.get(assignedSuperPeer.getIp(), assignedSuperPeer.getPort()));
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(node1.getIp(), node1.getPort()));
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(node2.getIp(), node2.getPort()));
        Assert.assertNotNull(ordinaryPeerRoutingTable.get(node3.getIp(), node3.getPort()));
    }
}
