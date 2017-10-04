package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex class.
 *
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class SuperPeerResourceIndexTestCase extends BaseTestCase {
    private String resourceName1;
    private String resourceName2;
    private String resourceName3;
    private String resourceName4;
    private SuperPeerResourceIndex superPeerResourceIndex;

    @BeforeMethod
    public void initializeMethod() {
        Node node1 = Mockito.mock(Node.class);
        Node node2 = Mockito.mock(Node.class);
        Node node3 = Mockito.mock(Node.class);
        Node node4 = Mockito.mock(Node.class);
        Node node5 = Mockito.mock(Node.class);
        Node node6 = Mockito.mock(Node.class);

        resourceName1 = "Lord of the Rings";
        resourceName2 = "Cars";
        resourceName3 = "Iron Man";
        resourceName4 = "Iron Man 2";

        superPeerResourceIndex = new SuperPeerResourceIndex();

        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName1, node1);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName1, node2);

        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName2, node4);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName2, node5);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName2, node6);

        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName3, node1);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName3, node3);

        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName4, node2);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName4, node3);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName4, node6);
    }

    @Test
    public void testAddResource() {
        String newAggreResourceResourceName = "Spider Man";
        Node newNode = Mockito.mock(Node.class);
        superPeerResourceIndex.addResourceToAggregatedIndex(newAggreResourceResourceName, newNode);

        Object ownedResourcesInternalState = Whitebox.getInternalState(superPeerResourceIndex, "aggregatedResources");
        Assert.assertTrue(ownedResourcesInternalState instanceof Set<?>);
        Set<?> resourceIndexOwnedResources = (Set<?>) ownedResourcesInternalState;
        Assert.assertEquals(resourceIndexOwnedResources.size(), 5);
        Assert.assertTrue(resourceIndexOwnedResources.contains(new AggregatedResource(newAggreResourceResourceName)));
    }

    @Test
    public void testFindAggregatedResources() {
        Set<AggregatedResource> ironManResources =
                superPeerResourceIndex.findAggregatedResources(resourceName3);

        Assert.assertEquals(ironManResources.size(), 2);
        Assert.assertFalse(ironManResources.contains(new AggregatedResource(resourceName1)));
        Assert.assertFalse(ironManResources.contains(new AggregatedResource(resourceName2)));
        Assert.assertTrue(ironManResources.contains(new AggregatedResource(resourceName3)));
        Assert.assertTrue(ironManResources.contains(new AggregatedResource(resourceName4)));
    }

    @Test
    public void testFindAggregatedResourceWithNoMatches() {
        Set<AggregatedResource> spiderManResources =
                superPeerResourceIndex.findAggregatedResources("Spider Man");

        Assert.assertEquals(spiderManResources.size(), 0);
    }

    @Test
    public void testFindAggregatedResourceWithDuplicates() {
        Node newNode = new Node();
        newNode.setIp("192.168.1.1");
        newNode.setPort(4067);
        newNode.setAlive(true);
        superPeerResourceIndex.addResourceToAggregatedIndex(resourceName2, newNode);

        Set<AggregatedResource> carsResources = superPeerResourceIndex.findAggregatedResources(resourceName2);
        AggregatedResource carsResource = carsResources.stream().findAny().orElse(null);

        Assert.assertEquals(carsResources.size(), 1);
        Assert.assertNotNull(carsResource);
        Assert.assertEquals(carsResource.getNodeCount(), 4);
        Assert.assertTrue(carsResource.getAllNodes().contains(newNode));
    }

    @Test
    public void testRemoveAggregatedResource() {
        Node newNode = new Node();
        newNode.setIp("192.168.1.1");
        newNode.setPort(4067);
        newNode.setAlive(true);
        String newResourceName = "Wonder Woman";

        superPeerResourceIndex.addResourceToAggregatedIndex(newResourceName, newNode);
        Set<AggregatedResource> wonderWomanResources = superPeerResourceIndex.findAggregatedResources(newResourceName);
        AggregatedResource wonderWomanResource = wonderWomanResources.stream().findAny().orElse(null);

        Assert.assertEquals(wonderWomanResources.size(), 1);
        Assert.assertNotNull(wonderWomanResource);
        Assert.assertEquals(wonderWomanResource.getNodeCount(), 1);
        Assert.assertTrue(wonderWomanResource.getAllNodes().contains(newNode));

        superPeerResourceIndex.removeResourceFromAggregatedIndex(newResourceName, newNode);
        Set<AggregatedResource> updatedWonderWomanResources =
                superPeerResourceIndex.findAggregatedResources(newResourceName);

        Assert.assertEquals(wonderWomanResource.getNodeCount(), 0);
        Assert.assertEquals(updatedWonderWomanResources.size(), 0);
    }
}
