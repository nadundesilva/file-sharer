package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy
 * class.
 */
public class UnstructuredFloodingRoutingStrategyTestCase extends BaseTestCase {
    private OrdinaryPeerRoutingTable routingTable;
    private UnstructuredFloodingRoutingStrategy unstructuredFloodingRoutingStrategy;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;

    @BeforeMethod
    public void initializeMethod() {
        unstructuredFloodingRoutingStrategy = new UnstructuredFloodingRoutingStrategy();

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable());

        fromNode = Mockito.mock(Node.class);
        node1 = Mockito.mock(Node.class);
        node2 = Mockito.mock(Node.class);
        node3 = Mockito.mock(Node.class);

        Set<Node> unstructuredNetworkNodes = new HashSet<>();
        unstructuredNetworkNodes.add(fromNode);
        Mockito.when(fromNode.isAlive()).thenReturn(true);
        unstructuredNetworkNodes.add(node1);
        Mockito.when(node1.isAlive()).thenReturn(true);
        unstructuredNetworkNodes.add(node2);
        Mockito.when(node2.isAlive()).thenReturn(true);
        unstructuredNetworkNodes.add(node3);
        Mockito.when(node3.isAlive()).thenReturn(true);
        Mockito.when(routingTable.getAllUnstructuredNetworkRoutingTableNodes())
                .thenReturn(unstructuredNetworkNodes);
    }

    @Test
    public void testName() {
        Assert.assertNotNull(unstructuredFloodingRoutingStrategy.getName());
    }

    @Test
    public void testGetForwardingNodes() {
        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 3);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }

    @Test
    public void testGetForwardingNodesInStartingNode() {
        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                null, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 4);
        Assert.assertTrue(forwardingNodes.contains(fromNode));
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }

    @Test
    public void testGetForwardingNodesWithDeadNodes() {
        Mockito.when(node1.isAlive()).thenReturn(false);

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }
}
