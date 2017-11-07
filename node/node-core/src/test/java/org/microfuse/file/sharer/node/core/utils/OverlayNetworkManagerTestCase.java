package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.udp.UDPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.utils.OverlayNetworkManager class.
 */
public class OverlayNetworkManagerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(OverlayNetworkManagerTestCase.class);

    private Router router;
    private OverlayNetworkManager overlayNetworkManager;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Overlay Network Manager Test");

        router = Mockito.spy(new Router(
                new UDPSocketNetworkHandler(serviceHolder),
                new UnstructuredFloodingRoutingStrategy(serviceHolder),
                serviceHolder
        ));
        Whitebox.setInternalState(serviceHolder, "router", router);
        overlayNetworkManager = new OverlayNetworkManager(serviceHolder);
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Overlay Network Manager Test");

        router.shutdown();
    }

    @Test(priority = 1)
    public void testConstructor() {
        logger.info("Running Overlay Network Manager Test 01 - Constructor");

        Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertNotNull(internalStateRouter);
        Assert.assertTrue(internalStateRouter == router);

        Object internalStateListenersList = Whitebox.getInternalState(router, "listenersList");
        Assert.assertTrue(internalStateListenersList instanceof List<?>);
        List<?> listenersList = (List<?>) internalStateListenersList;
        Assert.assertEquals(listenersList.size(), 1);
        Assert.assertTrue(listenersList.get(0) == overlayNetworkManager);
    }
}
