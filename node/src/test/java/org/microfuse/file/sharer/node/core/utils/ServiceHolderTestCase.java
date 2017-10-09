package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.utils.ServiceHolder class.
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
    public void testGetBootstrappingManagerAtFirstTime() {
        BootstrappingManager bootstrappingManager = ServiceHolder.getBootstrappingManager();

        Assert.assertNotNull(bootstrappingManager);

        Object routerInternalState = Whitebox.getInternalState(bootstrappingManager, "router");
        Assert.assertNotNull(routerInternalState);
        Object listenersListInternalState = Whitebox.getInternalState(routerInternalState, "listenersList");
        Assert.assertTrue(listenersListInternalState instanceof List<?>);
        List<?> routerListeners = (List<?>) listenersListInternalState;
        Assert.assertEquals(routerListeners.size(), 1);
        Assert.assertTrue(routerListeners.get(0) == bootstrappingManager);
    }

    @Test
    public void testGetBootstrappingManager() {
        BootstrappingManager initialBootstrappingManager = ServiceHolder.getBootstrappingManager();
        BootstrappingManager finalBootstrappingManager = ServiceHolder.getBootstrappingManager();

        Assert.assertNotNull(initialBootstrappingManager);
        Assert.assertNotNull(finalBootstrappingManager);
        Assert.assertTrue(initialBootstrappingManager == finalBootstrappingManager);
    }

    @Test
    public void testGetQueryManagerAtFirstTime() {
        QueryManager queryManager = ServiceHolder.getQueryManager();

        Assert.assertNotNull(queryManager);

        Object routerInternalState = Whitebox.getInternalState(queryManager, "router");
        Assert.assertNotNull(routerInternalState);
        Object listenersListInternalState = Whitebox.getInternalState(routerInternalState, "listenersList");
        Assert.assertTrue(listenersListInternalState instanceof List<?>);
        List<?> routerListeners = (List<?>) listenersListInternalState;
        Assert.assertEquals(routerListeners.size(), 1);
        Assert.assertTrue(routerListeners.get(0) == queryManager);
    }

    @Test
    public void testGetQueryManager() {
        QueryManager initialQueryManager = ServiceHolder.getQueryManager();
        QueryManager finalQueryManager = ServiceHolder.getQueryManager();

        Assert.assertNotNull(initialQueryManager);
        Assert.assertNotNull(finalQueryManager);
        Assert.assertTrue(initialQueryManager == finalQueryManager);
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