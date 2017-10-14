package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.AggregatedResource class.
 */
public class AggregatedResourceTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedResourceTestCase.class);

    private AggregatedResource aggregatedResource1;
    private AggregatedResource aggregatedResource2;
    private Node node1;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Aggregated Resource Test");

        node1 = Mockito.mock(Node.class);
        Node node2 = Mockito.mock(Node.class);
        Node node3 = Mockito.mock(Node.class);

        aggregatedResource1 = new AggregatedResource("Lord of the Rings");
        aggregatedResource1.addNode(node1);
        aggregatedResource1.addNode(node2);

        aggregatedResource2 = new AggregatedResource("Lord of the Rings");
        aggregatedResource2.addNode(node3);
    }

    @Test
    public void testEquals() {
        logger.info("Running Aggregated Resource Test 01 - Equals");

        Assert.assertTrue(aggregatedResource1.equals(aggregatedResource2));
    }

    @Test
    public void testHashCode() {
        logger.info("Running Aggregated Resource Test 02 - Hash code");

        Assert.assertEquals(aggregatedResource1.hashCode(), "Lord of the Rings".hashCode());
    }

    @Test
    public void testGetAllNodesCopying() {
        logger.info("Running Aggregated Resource Test 03 - Get all nodes copying");

        Set<Node> nodes = aggregatedResource1.getAllNodes();
        Object internalState = Whitebox.getInternalState(aggregatedResource1, "nodes");
        Assert.assertFalse(nodes == internalState);
    }
}
