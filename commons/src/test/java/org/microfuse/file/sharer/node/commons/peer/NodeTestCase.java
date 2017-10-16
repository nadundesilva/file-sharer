package org.microfuse.file.sharer.node.commons.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.commons.Node class.
 */
public class NodeTestCase {
    private static final Logger logger = LoggerFactory.getLogger(NodeTestCase.class);

    private Node node;
    private Node nodeCopy;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Node Test");

        node = new Node();
        node.setIp("192.168.1.1");
        node.setPort(4067);
        node.setAlive(true);

        nodeCopy = new Node();
        nodeCopy.setIp("192.168.1.1");
        nodeCopy.setPort("4067");
        nodeCopy.setAlive(false);
    }

    @Test(priority = 1)
    public void testEquals() {
        logger.info("Running Node Test 01 - Equals");

        Assert.assertTrue(node.equals(nodeCopy));
    }

    @Test(priority = 1)
    public void testHashCode() {
        logger.info("Running Node Test 02 - Hash code");

        Assert.assertEquals(node.hashCode(), (node.getIp() + ":" + node.getPort()).hashCode());
    }

    @Test(priority = 2)
    public void testIsAlive() {
        logger.info("Running Node Test 03 - Is alive");

        Assert.assertTrue(node.isAlive());
    }
}
