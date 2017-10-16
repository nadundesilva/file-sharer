package org.microfuse.file.sharer.node.core.resource.index;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.index.ResourceIndex class.
 * <p>
 * Cannot mock classes since hashCode() and equals() methods are used in tests.
 */
public class ResourceIndexTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(ResourceIndexTestCase.class);

    private OwnedResource ownedResource1;
    private OwnedResource ownedResource2;
    private OwnedResource ownedResource3;
    private OwnedResource ownedResource4;
    private ResourceIndex resourceIndex;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Resource Index Test");

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

    @Test(priority = 1)
    public void testAddResource() {
        logger.info("Running Resource Index Test 01 - Add resource");

        OwnedResource newOwnedResource = new OwnedResource("Spider Man");
        newOwnedResource.setFile(new File("movies" + File.separator + "spider_man.mp4"));
        resourceIndex.addResourceToIndex(newOwnedResource);

        Object ownedResourcesInternalState = Whitebox.getInternalState(resourceIndex, "ownedResources");
        Assert.assertTrue(ownedResourcesInternalState instanceof Set<?>);
        Set<?> resourceIndexOwnedResources = (Set<?>) ownedResourcesInternalState;
        Assert.assertEquals(resourceIndexOwnedResources.size(), 5);
        Assert.assertTrue(resourceIndexOwnedResources.contains(newOwnedResource));
    }

    @Test(priority = 2)
    public void testFindResources() {
        logger.info("Running Resource Index Test 02 - Find resources");

        Set<OwnedResource> ironManResources = resourceIndex.findResources(ownedResource3.getName());

        Assert.assertEquals(ironManResources.size(), 2);
        Assert.assertFalse(ironManResources.contains(ownedResource1));
        Assert.assertFalse(ironManResources.contains(ownedResource2));
        Assert.assertTrue(ironManResources.contains(ownedResource3));
        Assert.assertTrue(ironManResources.contains(ownedResource4));
    }

    @Test(priority = 3)
    public void testFindResourcesWithNoMatches() {
        logger.info("Running Resource Index Test 03 - Find resources with no matches");

        Set<OwnedResource> spiderManResources = resourceIndex.findResources("Spider Man");

        Assert.assertEquals(spiderManResources.size(), 0);
    }

    @Test(priority = 3)
    public void testFindResourcesWithDuplicates() {
        logger.info("Running Resource Index Test 04 - Find resources with duplicates");

        OwnedResource carsOwnedResource = new OwnedResource("Cars");
        carsOwnedResource.setFile(new File("downloaded" + File.separator + "cars.mp4"));
        resourceIndex.addResourceToIndex(carsOwnedResource);

        Set<OwnedResource> carsResources = resourceIndex.findResources("Cars");
        OwnedResource carsResource = carsResources.stream().findAny().orElse(null);

        Assert.assertEquals(carsResources.size(), 1);
        Assert.assertNotNull(carsResource);
        Assert.assertEquals(carsResource.getFile(), new File("downloaded" + File.separator + "cars.mp4"));
    }

    @Test(priority = 2)
    public void testRemoveLastNodeFromAggregatedResources() {
        logger.info("Running Resource Index Test 05 - Remove last node from aggregated resources");

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

    @Test(priority = 2)
    public void testGetAllResourcesCopying() {
        logger.info("Running Resource Index Test 06 - Get all resources copying");

        Set<OwnedResource> resources = resourceIndex.getAllResourcesInIndex();
        Object internalState = Whitebox.getInternalState(resourceIndex, "ownedResources");
        Assert.assertFalse(resources == internalState);
    }
}
