package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.lang.reflect.Field;

/**
 * The base test case class which needs to be extended by all test cases.
 */
public class BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BaseTestCase.class);

    protected ServiceHolder serviceHolder;

    @BeforeMethod
    public void baseInitializeMethod() {
        logger.info("Initializing Basic Test");

        serviceHolder = new ServiceHolder();
        clean();
    }

    @AfterMethod
    public void baseCleanUpMethod() {
        logger.info("Cleaning Up Basic Test");

        // Shutting down the router
        try {
            Field field = ServiceHolder.class.getDeclaredField("router");
            field.setAccessible(true);
            Object internalState = field.get(serviceHolder);
            field.setAccessible(false);

            if (internalState instanceof Router) {
                ((Router) internalState).shutdown();
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.warn("Test Case Cleanup: Failed to reset field router in "
                    + ServiceHolder.class.getName(), e);
        }
        serviceHolder.clear();
        clean();
    }

    /**
     * Clean the project.
     */
    private void clean() {
        new File(NodeConstants.CONFIG_FILE).delete();
    }

    /**
     * Wait for the specified number of milliseconds.
     *
     * @param milliseconds The milliseconds to wait for
     */
    protected void waitFor(int milliseconds) {
        try {
            logger.debug("Waiting for " + (milliseconds / 1000.0) + " seconds");
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            logger.warn("Failed to wait for " + milliseconds, e);
        }
    }
}
