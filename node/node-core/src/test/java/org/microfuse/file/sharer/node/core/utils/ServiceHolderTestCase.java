package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.TCPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
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
        logger.info("Running Service Holder Test 01 - Get configuration from file at first time");

        File configFile = new File(NodeConstants.CONFIG_FILE);
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

        Configuration configuration = serviceHolder.getConfiguration();

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
        logger.info("Running Service Holder Test 02 - Get default configuration at first time");

        Configuration configuration = serviceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getUsername(), NodeConstants.DEFAULT_USERNAME);
        Assert.assertEquals(configuration.getBootstrapServerIP(), NodeConstants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS);
        Assert.assertEquals(configuration.getBootstrapServerPort(), Constants.BOOTSTRAP_SERVER_PORT);
        Assert.assertEquals(configuration.getIp(), NodeConstants.DEFAULT_IP_ADDRESS);
        Assert.assertEquals(configuration.getPeerListeningPort(), NodeConstants.DEFAULT_TCP_LISTENER_PORT);
        Assert.assertEquals(configuration.getTimeToLive(), NodeConstants.DEFAULT_TIME_TO_LIVE);
        Assert.assertEquals(configuration.getListenerHandlingThreadCount(),
                NodeConstants.DEFAULT_LISTENER_HANDLER_THREAD_COUNT);
        Assert.assertEquals(configuration.getMaxAssignedOrdinaryPeerCount(),
                NodeConstants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT);
        Assert.assertEquals(configuration.getMaxUnstructuredPeerCount(),
                NodeConstants.DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT);
        Assert.assertEquals(configuration.getHeartBeatInterval(), NodeConstants.DEFAULT_HEART_BEAT_INTERVAL);
        Assert.assertEquals(configuration.getGossipingInterval(), NodeConstants.DEFAULT_GOSSIPING_INTERVAL);
        Assert.assertEquals(configuration.getNetworkHandlerTimeout(), NodeConstants.DEFAULT_NETWORK_HANDLER_TIMEOUT);
        Assert.assertEquals(configuration.getNetworkHandlerType(), NodeConstants.DEFAULT_NETWORK_HANDLER);
        Assert.assertEquals(configuration.getRoutingStrategyType(), NodeConstants.DEFAULT_ROUTING_STRATEGY);
    }

    @Test
    public void testGetConfigurationAtSecondTime() {
        logger.info("Running Service Holder Test 03 - Get configuration at second time");

        Configuration initialConfiguration = serviceHolder.getConfiguration();
        Configuration finalConfiguration = serviceHolder.getConfiguration();

        Assert.assertNotNull(initialConfiguration);
        Assert.assertNotNull(finalConfiguration);
        Assert.assertTrue(initialConfiguration == finalConfiguration);
    }

    @Test
    public void testGetResourceIndexAtFirstTime() {
        logger.info("Running Service Holder Test 04 - Get resource index at first time");

        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();

        Assert.assertNotNull(resourceIndex);
    }

    @Test
    public void testGetResourceIndexAtSecondTime() {
        logger.info("Running Service Holder Test 05 - Get resource index at second time");

        ResourceIndex initialResourceIndex = serviceHolder.getResourceIndex();
        ResourceIndex finalResourceIndex = serviceHolder.getResourceIndex();

        Assert.assertNotNull(initialResourceIndex);
        Assert.assertNotNull(finalResourceIndex);
        Assert.assertTrue(initialResourceIndex == finalResourceIndex);
    }

    @Test
    public void testGetOverlayNetworkManagerAtFirstTime() {
        logger.info("Running Service Holder Test 06 - Get overlay network manager at first time");

        OverlayNetworkManager overlayNetworkManager = serviceHolder.getOverlayNetworkManager();

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
    public void testGetOverlayNetworkManagerAtSecondTime() {
        logger.info("Running Service Holder Test 07 - Get overlay network manager at second time");

        OverlayNetworkManager initialOverlayNetworkManager = serviceHolder.getOverlayNetworkManager();
        OverlayNetworkManager finalOverlayNetworkManager = serviceHolder.getOverlayNetworkManager();

        Assert.assertNotNull(initialOverlayNetworkManager);
        Assert.assertNotNull(finalOverlayNetworkManager);
        Assert.assertTrue(initialOverlayNetworkManager == finalOverlayNetworkManager);
    }

    @Test
    public void testGetQueryManagerAtFirstTime() {
        logger.info("Running Service Holder Test 08 - Get query manager at first time");

        QueryManager queryManager = serviceHolder.getQueryManager();

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
    public void testGetQueryManagerAtSecondTime() {
        logger.info("Running Service Holder Test 09 - Get query manager at second time");

        QueryManager initialQueryManager = serviceHolder.getQueryManager();
        QueryManager finalQueryManager = serviceHolder.getQueryManager();

        Assert.assertNotNull(initialQueryManager);
        Assert.assertNotNull(finalQueryManager);
        Assert.assertTrue(initialQueryManager == finalQueryManager);
    }

    @Test
    public void testChangeNetworkHandler() {
        logger.info("Running Service Holder Test 10 - Change network handler");

        serviceHolder.getQueryManager();

        Router router = null;
        try {
            Field field = ServiceHolder.class.getDeclaredField("router");
            field.setAccessible(true);
            Object internalState = field.get(serviceHolder);
            Assert.assertTrue(internalState instanceof Router);
            router = (Router) internalState;
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail("Test Case Change Network Handler : Failed to fetch router.");
        }

        Assert.assertNotNull(router);

        NetworkHandler initialNetworkHandler = router.getNetworkHandler();
        serviceHolder.changeNetworkHandler(NetworkHandlerType.TCP_SOCKET);
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
        logger.info("Running Service Holder Test 11 - Change routing strategy");

        serviceHolder.getQueryManager();

        Router router = null;
        try {
            Field field = ServiceHolder.class.getDeclaredField("router");
            field.setAccessible(true);
            Object internalState = field.get(serviceHolder);
            Assert.assertTrue(internalState instanceof Router);
            router = (Router) internalState;
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Assert.fail("Test Case Change Routing Strategy : Failed to fetch router.");
        }

        Assert.assertNotNull(router);

        RoutingStrategy initialRoutingStrategy = router.getRoutingStrategy();
        serviceHolder.changeRoutingStrategy(RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK);
        RoutingStrategy finalRoutingStrategy = router.getRoutingStrategy();

        Assert.assertNotNull(initialRoutingStrategy);
        Assert.assertNotNull(finalRoutingStrategy);
        Assert.assertFalse(initialRoutingStrategy == finalRoutingStrategy);
        Assert.assertTrue(finalRoutingStrategy instanceof UnstructuredRandomWalkRoutingStrategy);
    }
}
