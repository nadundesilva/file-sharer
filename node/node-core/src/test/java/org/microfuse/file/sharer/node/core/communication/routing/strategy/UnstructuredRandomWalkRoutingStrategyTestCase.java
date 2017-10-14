package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(UnstructuredRandomWalkRoutingStrategyTestCase.class);

    private RoutingTable routingTable;
    private UnstructuredRandomWalkRoutingStrategy unstructuredRandomWalkRoutingStrategy;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Unstructured Random Walk Routing Strategy Test");

        unstructuredRandomWalkRoutingStrategy = new UnstructuredRandomWalkRoutingStrategy();

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable());

        fromNode = Mockito.mock(Node.class);
        node1 = Mockito.mock(Node.class);
        Mockito.when(node1.isAlive()).thenReturn(true);
        node2 = Mockito.mock(Node.class);
        Mockito.when(node2.isAlive()).thenReturn(true);
        node3 = Mockito.mock(Node.class);
        Mockito.when(node3.isAlive()).thenReturn(true);

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
        logger.info("Running Unstructured Random Walk Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(unstructuredRandomWalkRoutingStrategy.getName());
    }

    @Test
    public void testGetForwardingNodes() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 02 - Get forwarding nodes");

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1) || forwardingNodes.contains(node2) ||
                forwardingNodes.contains(node3));
    }

    @Test
    public void testGetForwardingNodesInStartingNode() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 03 - Get forwarding nodes " +
                "in the starting node");

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                null, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromNode) || forwardingNodes.contains(node1) ||
                forwardingNodes.contains(node2) || forwardingNodes.contains(node3));
    }

    @Test
    public void testGetForwardingNodesWithDeadNodes() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 04 - Get forwarding nodes " +
                "with dead nodes");

        Mockito.when(node1.isAlive()).thenReturn(false);

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node2) || forwardingNodes.contains(node3));
    }
}
