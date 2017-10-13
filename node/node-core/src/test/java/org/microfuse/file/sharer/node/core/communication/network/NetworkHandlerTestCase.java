package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
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
    private NetworkHandler networkHandler;
    private ExecutorService listenerHandlerExecutorService;

    @BeforeMethod
    public void initializeMethod() {
        // Mocking network handler
        networkHandler = Mockito.mock(NetworkHandler.class, Mockito.CALLS_REAL_METHODS);
        listenerHandlerExecutorService =
                Executors.newFixedThreadPool(ServiceHolder.getConfiguration().getListenerHandlingThreadCount());
        Whitebox.setInternalState(networkHandler, "listenerHandlerExecutorService",
                listenerHandlerExecutorService);
        Whitebox.setInternalState(networkHandler, "listenersListLock", new ReentrantReadWriteLock());
        Whitebox.setInternalState(networkHandler, "listenerHandlerExecutorServiceLock", new ReentrantReadWriteLock());
        Whitebox.setInternalState(networkHandler, "listenersList", new ArrayList());
        Whitebox.setInternalState(networkHandler, "restartRequired", false);
        Whitebox.setInternalState(networkHandler, "running", false);
    }

    @Test
    public void testStartListening() {
        networkHandler.startListening();

        Object internalState = Whitebox.getInternalState(networkHandler, "running");
        Assert.assertNotNull(internalState);
        Assert.assertTrue(internalState instanceof Boolean);
        Assert.assertTrue((Boolean) internalState);
    }

    @Test
    public void testRestart() {
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
