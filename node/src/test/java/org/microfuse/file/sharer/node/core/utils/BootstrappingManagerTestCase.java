package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.UDPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.NodeManager class.
 */
public class BootstrappingManagerTestCase extends BaseTestCase {
    private Router router;
    private BootstrappingManager bootstrappingManager;

    @BeforeMethod
    public void initializeMethod() {
        router = Mockito.spy(new Router(new UDPSocketNetworkHandler(), new UnstructuredFloodingRoutingStrategy()));
        bootstrappingManager = new BootstrappingManager(router);
    }

    @Test
    public void testConstructor() {
        Object internalStateRouter = Whitebox.getInternalState(bootstrappingManager, "router");
        Assert.assertNotNull(internalStateRouter);
        Assert.assertTrue(internalStateRouter == router);

        Object internalStateListenersList = Whitebox.getInternalState(router, "listenersList");
        Assert.assertTrue(internalStateListenersList instanceof List<?>);
        List<?> listenersList = (List<?>) internalStateListenersList;
        Assert.assertEquals(listenersList.size(), 1);
        Assert.assertTrue(listenersList.get(0) == bootstrappingManager);
    }
}
