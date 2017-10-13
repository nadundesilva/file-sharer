package org.microfuse.file.sharer.node.commons;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.commons.Node class.
 */
public class NodeTestCase extends BaseTestCase {
    private Node node;
    private Node nodeCopy;

    @BeforeMethod
    public void initializeMethod() {
        node = new Node();
        node.setIp("192.168.1.1");
        node.setPort(4067);
        node.setAlive(true);

        nodeCopy = new Node();
        nodeCopy.setIp("192.168.1.1");
        nodeCopy.setPort("4067");
        nodeCopy.setAlive(false);
    }

    @Test
    public void testIsAlive() {
        Assert.assertTrue(node.isAlive());
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(node.equals(nodeCopy));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(node.hashCode(), (node.getIp() + ":" + node.getPort()).hashCode());
    }
}
