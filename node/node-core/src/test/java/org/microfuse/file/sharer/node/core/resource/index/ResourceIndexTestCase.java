package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.index.ResourceIndex class.
 *
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class ResourceIndexTestCase extends BaseTestCase {
    private OwnedResource ownedResource1;
    private OwnedResource ownedResource2;
    private OwnedResource ownedResource3;
    private OwnedResource ownedResource4;
    private ResourceIndex resourceIndex;

    @BeforeMethod
    public void initializeMethod() {
        resourceIndex = new ResourceIndex();

        ownedResource1 = new OwnedResource("Lord of the Rings");
        ownedResource1.setFile(new File("movies" + File.separator + "lord_of_the_rings.mp4"));
        resourceIndex.addResourceToIndex(ownedResource1);

        ownedResource2 = new OwnedResource("Cars");
        ownedResource2.setFile(new File("movies" + File.separator + "cars.mp4"));
        resourceIndex.addResourceToIndex(ownedResource2);

        ownedResource3 = new OwnedResource("Iron Man");
        ownedResource3.setFile(new File("movies" + File.separator + "iron_man.mp4"));
        resourceIndex.addResourceToIndex(ownedResource3);

        ownedResource4 = new OwnedResource("Iron Man 2");
        ownedResource4.setFile(new File("movies" + File.separator + "iron_man_2.mp4"));
        resourceIndex.addResourceToIndex(ownedResource4);
    }

    @Test
    public void testAddResource() {
        OwnedResource newOwnedResource = new OwnedResource("Spider Man");
        newOwnedResource.setFile(new File("movies" + File.separator + "spider_man.mp4"));
        resourceIndex.addResourceToIndex(newOwnedResource);

        Object ownedResourcesInternalState = Whitebox.getInternalState(resourceIndex, "ownedResources");
        Assert.assertTrue(ownedResourcesInternalState instanceof Set<?>);
        Set<?> resourceIndexOwnedResources = (Set<?>) ownedResourcesInternalState;
        Assert.assertEquals(resourceIndexOwnedResources.size(), 5);
        Assert.assertTrue(resourceIndexOwnedResources.contains(newOwnedResource));
    }

    @Test
    public void testFindResources() {
        Set<OwnedResource> ironManResources = resourceIndex.findResources(ownedResource3.getName());

        Assert.assertEquals(ironManResources.size(), 2);
        Assert.assertFalse(ironManResources.contains(ownedResource1));
        Assert.assertFalse(ironManResources.contains(ownedResource2));
        Assert.assertTrue(ironManResources.contains(ownedResource3));
        Assert.assertTrue(ironManResources.contains(ownedResource4));
    }

    @Test
    public void testFindResourceWithNoMatches() {
        Set<OwnedResource> spiderManResources = resourceIndex.findResources("Spider Man");

        Assert.assertEquals(spiderManResources.size(), 0);
    }

    @Test
    public void testFindResourceWithDuplicates() {
        OwnedResource carsOwnedResource = new OwnedResource("Cars");
        carsOwnedResource.setFile(new File("downloaded" + File.separator + "cars.mp4"));
        resourceIndex.addResourceToIndex(carsOwnedResource);

        Set<OwnedResource> carsResources = resourceIndex.findResources("Cars");
        OwnedResource carsResource = carsResources.stream().findAny().orElse(null);

        Assert.assertEquals(carsResources.size(), 1);
        Assert.assertNotNull(carsResource);
        Assert.assertEquals(carsResource.getFile(), new File("downloaded" + File.separator + "cars.mp4"));
    }

    @Test
    public void testRemoveLastNodeFromAggregatedResources() {
        OwnedResource newOwnedResource = new OwnedResource("Wonder Woman");
        newOwnedResource.setFile(new File("movies" + File.separator + "wonder_woman.mp4"));
        resourceIndex.addResourceToIndex(newOwnedResource);

        resourceIndex.addResourceToIndex(newOwnedResource);
        Set<OwnedResource> wonderWomanResources = resourceIndex.findResources(newOwnedResource.getName());
        OwnedResource wonderWomanResource = wonderWomanResources.stream().findAny().orElse(null);

        Assert.assertEquals(wonderWomanResources.size(), 1);
        Assert.assertNotNull(wonderWomanResource);
        Assert.assertEquals(wonderWomanResource, newOwnedResource);
        Assert.assertEquals(wonderWomanResource.getFile(), new File("movies" + File.separator + "wonder_woman.mp4"));

        resourceIndex.removeResourceFromIndex(newOwnedResource.getName());
        Set<OwnedResource> updatedWonderWomanResources = resourceIndex.findResources(newOwnedResource.getName());

        Assert.assertEquals(wonderWomanResource, newOwnedResource);
        Assert.assertEquals(updatedWonderWomanResources.size(), 0);
    }

    @Test
    public void testGetAllResourcesCopying() {
        Set<OwnedResource> resources = resourceIndex.getAllResourcesInIndex();
        Object internalState = Whitebox.getInternalState(resourceIndex, "ownedResources");
        Assert.assertFalse(resources == internalState);
    }
}
