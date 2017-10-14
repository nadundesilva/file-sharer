package org.microfuse.file.sharer.node.core.resource;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test Case for org.microfuse.file.sharer.node.core.resource.OwnedResource class.
 */
public class OwnedResourceTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(OwnedResourceTestCase.class);

    private OwnedResource ownedResource;
    private OwnedResource ownedResourceCopy;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Owned Resource Test");

        ownedResource = new OwnedResource("Lord of the Rings");
        ownedResource.setFile(new File("downloaded" + File.separator + "lord_of_the_rings.mp4"));

        ownedResourceCopy = new OwnedResource("Lord of the Rings");
        ownedResourceCopy.setFile(new File("movies" + File.separator + "lord_of_the_rings.mp4"));
    }

    @Test
    public void testEquals() {
        logger.info("Running Owned Resource Test 01 - Equals");

        Assert.assertTrue(ownedResource.equals(ownedResourceCopy));
    }

    @Test
    public void testHashCode() {
        logger.info("Running Owned Resource Test 02 - Hash code");

        Assert.assertEquals(ownedResource.hashCode(), "Lord of the Rings".hashCode());
    }
}
