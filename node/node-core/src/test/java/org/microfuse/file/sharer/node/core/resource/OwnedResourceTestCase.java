package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.OwnedResource class.
 */
public class OwnedResourceTestCase extends BaseTestCase {
    private OwnedResource ownedResource;
    private OwnedResource ownedResourceCopy;

    @BeforeMethod
    public void initializeMethod() {
        ownedResource = new OwnedResource("Lord of the Rings");
        ownedResource.setFile(new File("downloaded" + File.separator + "lord_of_the_rings.mp4"));

        ownedResourceCopy = new OwnedResource("Lord of the Rings");
        ownedResourceCopy.setFile(new File("movies" + File.separator + "lord_of_the_rings.mp4"));
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(ownedResource.equals(ownedResourceCopy));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(ownedResource.hashCode(), "Lord of the Rings".hashCode());
    }
}
