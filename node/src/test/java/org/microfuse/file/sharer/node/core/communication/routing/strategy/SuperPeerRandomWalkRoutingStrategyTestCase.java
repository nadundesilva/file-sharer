package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.Manager;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.mockito.Mockito;
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
        superPeerRandomWalkRoutingStrategy = new SuperPeerRandomWalkRoutingStrategy();

        ordinaryPeerRoutingTable = Mockito.spy(new OrdinaryPeerRoutingTable());
        superPeerRoutingTable = Mockito.spy(new SuperPeerRoutingTable());
        queryResourceName = "Lord of the Rings";

        fromNode = Mockito.mock(Node.class);
        fromSuperPeerNode = Mockito.mock(Node.class);
        node1 = Mockito.mock(Node.class);
        node2 = Mockito.mock(Node.class);
        node3 = Mockito.mock(Node.class);
        node4 = Mockito.mock(Node.class);
        node5 = Mockito.mock(Node.class);

        Set<Node> unstructuredNetworkNode = new HashSet<>();
        unstructuredNetworkNode.add(fromSuperPeerNode);
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
        assignedOrdinaryPeerNodes.add(node1);
        assignedOrdinaryPeerNodes.add(node2);
        assignedOrdinaryPeerNodes.add(node3);
        Mockito.when(superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes())
                .thenReturn(assignedOrdinaryPeerNodes);

        Set<Node> superPeerNetworkNodes = new HashSet<>();
        superPeerNetworkNodes.add(fromSuperPeerNode);
        superPeerNetworkNodes.add(node4);
        superPeerNetworkNodes.add(node5);
        Mockito.when(superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes())
                .thenReturn(superPeerNetworkNodes);
    }

    @Test
    public void testName() {
        Assert.assertNotNull(superPeerRandomWalkRoutingStrategy.getName());
    }

    @Test
    public void testGetForwardingNodesInOrdinaryPeerWithAssignedSuperPeer() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(ordinaryPeerRoutingTable.getAssignedSuperPeer()).thenReturn(node1);
        Mockito.when(node1.isAlive()).thenReturn(true);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1));
    }

    @Test
    public void testGetForwardingNodesInOrdinaryPeerWithUnassignedSuperPeer() {
        Message message = Mockito.mock(Message.class);
        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(ordinaryPeerRoutingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertFalse(forwardingNodes.contains(fromNode));
    }

    @Test
    public void testGetForwardingNodesInSuperPeerWithResourceInAssignedOrdinaryPeer() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        Manager.promoteToSuperPeer();
        ResourceIndex resourceIndex = Manager.getResourceIndex();
        resourceIndex.clear();

        Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
        SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;

        superPeerResourceIndex.addResourceToAggregatedIndex(queryResourceName, node1);
        superPeerResourceIndex.addResourceToAggregatedIndex(queryResourceName, node2);

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertFalse(forwardingNodes.contains(fromSuperPeerNode));
    }

    @Test
    public void testGetForwardingNodesInSuperPeerWithResourceNotInAssignedOrdinaryPeer() {
        Message message = Mockito.mock(Message.class);
        Mockito.when(message.getData(MessageIndexes.SER_FILE_NAME)).thenReturn(queryResourceName);

        Manager.promoteToSuperPeer();
        ResourceIndex resourceIndex = Manager.getResourceIndex();
        resourceIndex.clear();

        Set<Node> forwardingNodes = superPeerRandomWalkRoutingStrategy.getForwardingNodes(superPeerRoutingTable,
                fromSuperPeerNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertFalse(forwardingNodes.contains(fromSuperPeerNode));
    }
}
