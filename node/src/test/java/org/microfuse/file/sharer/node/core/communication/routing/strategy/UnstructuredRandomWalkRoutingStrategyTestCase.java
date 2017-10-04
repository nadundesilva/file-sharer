package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for
 * org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredRandomWalkRoutingStrategy class.
 */
public class UnstructuredRandomWalkRoutingStrategyTestCase extends BaseTestCase {
    private RoutingTable routingTable;
    private UnstructuredRandomWalkRoutingStrategy unstructuredRandomWalkRoutingStrategy;
    private Node fromNode;

    @BeforeMethod
    public void initializeMethod() {
        unstructuredRandomWalkRoutingStrategy = new UnstructuredRandomWalkRoutingStrategy();

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable());

        fromNode = Mockito.mock(Node.class);
        Node node1 = Mockito.mock(Node.class);
        Node node2 = Mockito.mock(Node.class);
        Node node3 = Mockito.mock(Node.class);

        Set<Node> unstructuredNetworkNodes = new HashSet<>();
        unstructuredNetworkNodes.add(fromNode);
        unstructuredNetworkNodes.add(node1);
        unstructuredNetworkNodes.add(node2);
        unstructuredNetworkNodes.add(node3);
        Mockito.when(routingTable.getAllUnstructuredNetworkRoutingTableNodes())
                .thenReturn(unstructuredNetworkNodes);
    }

    @Test
    public void testName() {
        Assert.assertNotNull(unstructuredRandomWalkRoutingStrategy.getName());
    }

    @Test
    public void testGetForwardingNodes() {
        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertFalse(forwardingNodes.contains(fromNode));
    }
}
