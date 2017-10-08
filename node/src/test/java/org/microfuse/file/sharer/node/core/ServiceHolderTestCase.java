package org.microfuse.file.sharer.node.core;

import com.google.common.io.Files;
import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.ServiceHolder class.
 */
public class ServiceHolderTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHolderTestCase.class);

    @Test
    public void testGetConfigurationFromFileAtFirstTime() {
        File configFile = new File(Constants.CONFIG_FILE);
        String configString = "{" +
                "\"ip\":\"192.168.1.3\"," +
                "\"peerListeningPort\":4562," +
                "\"timeToLive\":3," +
                "\"networkHandlerType\":\"TCP_SOCKET\"," +
                "\"routingStrategyType\":\"SUPER_PEER_RANDOM_WALK\"," +
                "\"peerType\":\"SUPER_PEER\"" +
                "}";
        try {
            Files.write(configString.getBytes(), configFile);
        } catch (IOException e) {
            logger.warn("Failed to create config file. Failed to test get configuration from file.");
        }

        Configuration configuration = ServiceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getIp(), "192.168.1.3");
        Assert.assertEquals(configuration.getPeerListeningPort(), 4562);
        Assert.assertEquals(configuration.getTimeToLive(), 3);
        Assert.assertEquals(configuration.getNetworkHandlerType(), NetworkHandlerType.TCP_SOCKET);
        Assert.assertEquals(configuration.getRoutingStrategyType(), RoutingStrategyType.SUPER_PEER_RANDOM_WALK);
    }

    @Test
    public void testGetDefaultConfigurationAtFirstTime() {
        Configuration configuration = ServiceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getIp(), Constants.DEFAULT_IP_ADDRESS);
        Assert.assertEquals(configuration.getPeerListeningPort(), Constants.DEFAULT_TCP_LISTENER_PORT);
        Assert.assertEquals(configuration.getTimeToLive(), Constants.DEFAULT_TIME_TO_LIVE);
        Assert.assertEquals(configuration.getNetworkHandlerType(), Constants.DEFAULT_NETWORK_HANDLER);
        Assert.assertEquals(configuration.getRoutingStrategyType(), Constants.DEFAULT_ROUTING_STRATEGY);
    }

    @Test
    public void testGetConfiguration() {
        Configuration initialConfiguration = ServiceHolder.getConfiguration();
        Configuration finalConfiguration = ServiceHolder.getConfiguration();

        Assert.assertNotNull(initialConfiguration);
        Assert.assertNotNull(finalConfiguration);
        Assert.assertTrue(initialConfiguration == finalConfiguration);
    }

    @Test
    public void testGetRouterAtFirstTime() {
        Router router = ServiceHolder.getRouter();

        Assert.assertNotNull(router);

        Object routingStrategyInternalState = Whitebox.getInternalState(router, "routingStrategy");
        Assert.assertNotNull(routingStrategyInternalState);

        Object networkHandlerInternalState = Whitebox.getInternalState(router, "networkHandler");
        Assert.assertNotNull(networkHandlerInternalState);
        Assert.assertTrue(networkHandlerInternalState instanceof NetworkHandler);
        NetworkHandler networkHandler = (NetworkHandler) networkHandlerInternalState;
        Object networkHandlerListenersInternalList = Whitebox.getInternalState(networkHandler, "listenersList");
        Assert.assertTrue(networkHandlerListenersInternalList instanceof List<?>);
        List<?> networkHandlerListeners = (List<?>) networkHandlerListenersInternalList;
        Assert.assertEquals(networkHandlerListeners.size(), 1);
        Assert.assertTrue(networkHandlerListeners.get(0) == router);
    }

    @Test
    public void testGetRouter() {
        Router initialRouter = ServiceHolder.getRouter();
        Router finalRouter = ServiceHolder.getRouter();

        Assert.assertNotNull(initialRouter);
        Assert.assertNotNull(finalRouter);
        Assert.assertTrue(initialRouter == finalRouter);
    }

    @Test
    public void testGetResourceIndexAtFirstTime() {
        ResourceIndex resourceIndex = ServiceHolder.getResourceIndex();

        Assert.assertNotNull(resourceIndex);
    }

    @Test
    public void testGetResourceIndex() {
        ResourceIndex initialResourceIndex = ServiceHolder.getResourceIndex();
        ResourceIndex finalResourceIndex = ServiceHolder.getResourceIndex();

        Assert.assertNotNull(initialResourceIndex);
        Assert.assertNotNull(finalResourceIndex);
        Assert.assertTrue(initialResourceIndex == finalResourceIndex);
    }
}