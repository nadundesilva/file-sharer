package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.network.NetworkHandler class.
 */
public class NetworkHandlerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandlerTestCase.class);

    private NetworkHandler networkHandler;
    private ExecutorService listenerHandlerExecutorService;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Network Handler Test");

        // Mocking network handler
        networkHandler = Mockito.mock(NetworkHandler.class, Mockito.CALLS_REAL_METHODS);
        Whitebox.setInternalState(networkHandler, "serviceHolder", serviceHolder);

        listenerHandlerExecutorService =
                Executors.newFixedThreadPool(serviceHolder.getConfiguration().getListenerHandlingThreadCount());
        Whitebox.setInternalState(networkHandler, "listenerHandlerExecutorService",
                listenerHandlerExecutorService);
        Whitebox.setInternalState(networkHandler, "listenersListLock", new ReentrantReadWriteLock());
        Whitebox.setInternalState(networkHandler, "listenerHandlerExecutorServiceLock", new ReentrantReadWriteLock());
        Whitebox.setInternalState(networkHandler, "listenersList", new ArrayList());
        Whitebox.setInternalState(networkHandler, "restartRequired", false);
        Whitebox.setInternalState(networkHandler, "running", false);
    }

    @Test(priority = 1)
    public void testStartListening() {
        logger.info("Running Network Handler Test 01 - Start listening");

        networkHandler.startListening();

        Object internalState = Whitebox.getInternalState(networkHandler, "running");
        Assert.assertNotNull(internalState);
        Assert.assertTrue(internalState instanceof Boolean);
        Assert.assertTrue((Boolean) internalState);
    }

    @Test(priority = 2)
    public void testRestart() {
        logger.info("Running Network Handler Test 02 - Restart");

        networkHandler.restart();

        Object internalStateRestartRequired = Whitebox.getInternalState(networkHandler, "restartRequired");
        Assert.assertNotNull(internalStateRestartRequired);
        Assert.assertTrue(internalStateRestartRequired instanceof Boolean);
        Assert.assertFalse((Boolean) internalStateRestartRequired);

        Object internalStateExecutorService = Whitebox.getInternalState(networkHandler, "restartRequired");
        Assert.assertNotNull(internalStateExecutorService);
        waitFor(2000);
        Assert.assertTrue(listenerHandlerExecutorService.isShutdown());
        Assert.assertFalse(listenerHandlerExecutorService == internalStateExecutorService);
    }
}
