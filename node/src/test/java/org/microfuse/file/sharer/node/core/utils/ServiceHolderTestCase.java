package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import org.microfuse.file.sharer.node.BaseTestCase;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.network.TCPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredRandomWalkRoutingStrategy;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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
                "\"username\":\"microfuse.tester\"," +
                "\"bootstrapServerIP\":\"192.168.1.15\"," +
                "\"bootstrapServerPort\":5555," +
                "\"ip\":\"192.168.1.3\"," +
                "\"peerListeningPort\":4562," +
                "\"timeToLive\":3," +
                "\"listenerHandlingThreadCount\":14," +
                "\"maxAssignedOrdinaryPeerCount\":13," +
                "\"maxUnstructuredPeerCount\":24," +
                "\"heartBeatInterval\":56," +
                "\"gossipingInterval\":72," +
                "\"networkHandlerTimeout\":23," +
                "\"networkHandlerType\":\"TCP_SOCKET\"," +
                "\"routingStrategyType\":\"SUPER_PEER_RANDOM_WALK\"" +
                "}";
        try {
            Files.write(configString.getBytes(), configFile);
        } catch (IOException e) {
            logger.warn("Failed to create config file. Failed to test get configuration from file.");
        }

        Configuration configuration = ServiceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getUsername(), "microfuse.tester");
        Assert.assertEquals(configuration.getBootstrapServerIP(), "192.168.1.15");
        Assert.assertEquals(configuration.getBootstrapServerPort(), 5555);
        Assert.assertEquals(configuration.getIp(), "192.168.1.3");
        Assert.assertEquals(configuration.getPeerListeningPort(), 4562);
        Assert.assertEquals(configuration.getTimeToLive(), 3);
        Assert.assertEquals(configuration.getListenerHandlingThreadCount(), 14);
        Assert.assertEquals(configuration.getMaxAssignedOrdinaryPeerCount(), 13);
        Assert.assertEquals(configuration.getMaxUnstructuredPeerCount(), 24);
        Assert.assertEquals(configuration.getHeartBeatInterval(), 56);
        Assert.assertEquals(configuration.getGossipingInterval(), 72);
        Assert.assertEquals(configuration.getNetworkHandlerTimeout(), 23);
        Assert.assertEquals(configuration.getNetworkHandlerType(), NetworkHandlerType.TCP_SOCKET);
        Assert.assertEquals(configuration.getRoutingStrategyType(), RoutingStrategyType.SUPER_PEER_RANDOM_WALK);
    }

    @Test
    public void testGetDefaultConfigurationAtFirstTime() {
        Configuration configuration = ServiceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getUsername(), Constants.DEFAULT_USERNAME);
        Assert.assertEquals(configuration.getBootstrapServerIP(), Constants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS);
        Assert.assertEquals(configuration.getBootstrapServerPort(), Constants.DEFAULT_BOOTSTRAP_SERVER_PORT);
        Assert.assertEquals(configuration.getIp(), Constants.DEFAULT_IP_ADDRESS);
        Assert.assertEquals(configuration.getPeerListeningPort(), Constants.DEFAULT_TCP_LISTENER_PORT);
        Assert.assertEquals(configuration.getTimeToLive(), Constants.DEFAULT_TIME_TO_LIVE);
        Assert.assertEquals(configuration.getListenerHandlingThreadCount(),
                Constants.DEFAULT_LISTENER_HANDLER_THREAD_COUNT);
        Assert.assertEquals(configuration.getMaxAssignedOrdinaryPeerCount(),
                Constants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT);
        Assert.assertEquals(configuration.getMaxUnstructuredPeerCount(), Constants.DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT);
        Assert.assertEquals(configuration.getHeartBeatInterval(), Constants.DEFAULT_HEART_BEAT_INTERVAL);
        Assert.assertEquals(configuration.getGossipingInterval(), Constants.DEFAULT_GOSSIPING_INTERVAL);
        Assert.assertEquals(configuration.getNetworkHandlerTimeout(), Constants.DEFAULT_NETWORK_HANDLER_TIMEOUT);
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

    @Test
    public void testGetOverlayNetworkManagerAtFirstTime() {
        OverlayNetworkManager overlayNetworkManager = ServiceHolder.getOverlayNetworkManager();

        Assert.assertNotNull(overlayNetworkManager);

        Object routerInternalState = Whitebox.getInternalState(overlayNetworkManager, "router");
        Assert.assertNotNull(routerInternalState);
        Object listenersListInternalState = Whitebox.getInternalState(routerInternalState, "listenersList");
        Assert.assertTrue(listenersListInternalState instanceof List<?>);
        List<?> routerListeners = (List<?>) listenersListInternalState;
        Assert.assertEquals(routerListeners.size(), 1);
        Assert.assertTrue(routerListeners.get(0) == overlayNetworkManager);
    }

    @Test
    public void testGetOverlayNetworkManager() {
        OverlayNetworkManager initialOverlayNetworkManager = ServiceHolder.getOverlayNetworkManager();
        OverlayNetworkManager finalOverlayNetworkManager = ServiceHolder.getOverlayNetworkManager();

        Assert.assertNotNull(initialOverlayNetworkManager);
        Assert.assertNotNull(finalOverlayNetworkManager);
        Assert.assertTrue(initialOverlayNetworkManager == finalOverlayNetworkManager);
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
    public void testChangeNetworkHandler() {
        ServiceHolder.getQueryManager();

        Router router = null;
        try {
            Field field = ServiceHolder.class.getDeclaredField("router");
            field.setAccessible(true);
            Object internalState = field.get(ServiceHolder.class);
            Assert.assertTrue(internalState instanceof Router);
            router = (Router) internalState;
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail("Test Case Change Network Handler : Failed to fetch router.");
        }

        Assert.assertNotNull(router);

        NetworkHandler initialNetworkHandler = router.getNetworkHandler();
        ServiceHolder.changeNetworkHandler(NetworkHandlerType.TCP_SOCKET);
        NetworkHandler finalNetworkHandler = router.getNetworkHandler();

        Assert.assertNotNull(initialNetworkHandler);
        Assert.assertNotNull(finalNetworkHandler);
        Assert.assertFalse(initialNetworkHandler == finalNetworkHandler);
        Assert.assertTrue(finalNetworkHandler instanceof TCPSocketNetworkHandler);

        initialNetworkHandler.shutdown();
        finalNetworkHandler.shutdown();
        waitFor(1000);
    }

    @Test
    public void testChangeRoutingStrategy() {
        ServiceHolder.getQueryManager();

        Router router = null;
        try {
            Field field = ServiceHolder.class.getDeclaredField("router");
            field.setAccessible(true);
            Object internalState = field.get(ServiceHolder.class);
            Assert.assertTrue(internalState instanceof Router);
            router = (Router) internalState;
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail("Test Case Change Routing Strategy : Failed to fetch router.");
        }

        Assert.assertNotNull(router);

        RoutingStrategy initialRoutingStrategy = router.getRoutingStrategy();
        ServiceHolder.changeRoutingStrategy(RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK);
        RoutingStrategy finalRoutingStrategy = router.getRoutingStrategy();

        Assert.assertNotNull(initialRoutingStrategy);
        Assert.assertNotNull(finalRoutingStrategy);
        Assert.assertFalse(initialRoutingStrategy == finalRoutingStrategy);
        Assert.assertTrue(finalRoutingStrategy instanceof UnstructuredRandomWalkRoutingStrategy);
    }
}
