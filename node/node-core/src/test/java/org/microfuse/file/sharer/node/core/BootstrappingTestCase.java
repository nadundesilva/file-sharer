package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Test Case for testing interactions with the bootstrap server.
 */
public class BootstrappingTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BootstrappingTestCase.class);

    private BootstrapServer bootstrapServer;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Bootstrapping Test");

        bootstrapServer = new BootstrapServer();
        bootstrapServer.start();
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Bootstrapping Test");

        bootstrapServer.shutdown();
    }
}
