package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(UnstructuredFloodingRoutingStrategyTestCase.class);

    private OrdinaryPeerRoutingTable routingTable;
    private UnstructuredFloodingRoutingStrategy unstructuredFloodingRoutingStrategy;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Unstructured Flooding Routing Strategy Test");

        unstructuredFloodingRoutingStrategy = new UnstructuredFloodingRoutingStrategy(serviceHolder);

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));

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
        logger.info("Running Unstructured Flooding Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(unstructuredFloodingRoutingStrategy.getName());
    }

    @Test
    public void testGetForwardingNodes() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 02 - Get forwarding nodes");

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 3);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }

    @Test
    public void testGetForwardingNodesInStartingNode() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 03 - Get forwarding nodes " +
                "in the starting node");

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
        logger.info("Running Unstructured Flooding Routing Strategy Test 04 - Get forwarding nodes " +
                "with dead nodes");

        Mockito.when(node1.isAlive()).thenReturn(false);

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, Mockito.mock(Message.class));

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }
}
