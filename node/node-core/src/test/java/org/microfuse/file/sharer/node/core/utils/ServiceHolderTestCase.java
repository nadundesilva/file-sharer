package org.microfuse.file.sharer.node.core.utils;

import com.google.common.io.Files;
import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.udp.UDPSocketNetworkHandler;
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

    @Test(priority = 1)
    public void testGetConfigurationFromFileAtFirstTime() {
        logger.info("Running Service Holder Test 01 - Get configuration from file at first time");

        File configFile = new File(NodeConstants.CONFIG_FILE);
        String configString = "{" +
                "\"bootstrapServerIP\":\"192.168.1.15\"," +
                "\"bootstrapServerPort\":4765," +
                "\"username\":\"microfuse.tester\"," +
                "\"ip\":\"192.168.1.3\"," +
                "\"peerListeningPort\":4562," +
                "\"listenerHandlingThreadCount\":14," +
                "\"timeToLive\":3," +
                "\"maxAssignedOrdinaryPeerCount\":13," +
                "\"maxUnstructuredPeerCount\":24," +
                "\"heartbeatInterval\":56000," +
                "\"gossipingInterval\":72000," +
                "\"networkHandlerSendTimeout\":23000," +
                "\"networkHandlerThreadCount\":14," +
                "\"networkHandlerReplyTimeout\":46000," +
                "\"serSuperPeerTimeout\":74562," +
                "\"bootstrapServerReplyWaitTimeout\":465132," +
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
        Assert.assertEquals(configuration.getBootstrapServerIP(), "192.168.1.15");
        Assert.assertEquals(configuration.getBootstrapServerPort(), 4765);
        Assert.assertEquals(configuration.getUsername(), "microfuse.tester");
        Assert.assertEquals(configuration.getIp(), "192.168.1.3");
        Assert.assertEquals(configuration.getPeerListeningPort(), 4562);
        Assert.assertEquals(configuration.getNetworkHandlerThreadCount(), 14);
        Assert.assertEquals(configuration.getTimeToLive(), 3);
        Assert.assertEquals(configuration.getMaxAssignedOrdinaryPeerCount(), 13);
        Assert.assertEquals(configuration.getMaxUnstructuredPeerCount(), 24);
        Assert.assertEquals(configuration.getHeartbeatInterval(), 56000);
        Assert.assertEquals(configuration.getGossipingInterval(), 72000);
        Assert.assertEquals(configuration.getNetworkHandlerSendTimeout(), 23000);
        Assert.assertEquals(configuration.getBootstrapServerReplyWaitTimeout(), 465132);
        Assert.assertEquals(configuration.getSerSuperPeerTimeout(), 74562);
        Assert.assertEquals(configuration.getNetworkHandlerType(), NetworkHandlerType.TCP_SOCKET);
        Assert.assertEquals(configuration.getRoutingStrategyType(), RoutingStrategyType.SUPER_PEER_RANDOM_WALK);
    }

    @Test(priority = 1)
    public void testGetDefaultConfigurationAtFirstTime() {
        logger.info("Running Service Holder Test 02 - Get default configuration at first time");

        Configuration configuration = serviceHolder.getConfiguration();

        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.getUsername(), NodeConstants.DEFAULT_USERNAME);
        Assert.assertEquals(configuration.getBootstrapServerIP(), NodeConstants.DEFAULT_BOOTSTRAP_SERVER_IP_ADDRESS);
        Assert.assertEquals(configuration.getBootstrapServerPort(), Constants.BOOTSTRAP_SERVER_PORT);
        Assert.assertEquals(configuration.getIp(), NodeConstants.DEFAULT_IP_ADDRESS);
        Assert.assertEquals(configuration.getPeerListeningPort(), NodeConstants.DEFAULT_PEER_LISTENING_PORT);
        Assert.assertEquals(configuration.getTimeToLive(), NodeConstants.DEFAULT_TIME_TO_LIVE);
        Assert.assertEquals(configuration.getNetworkHandlerThreadCount(),
                NodeConstants.DEFAULT_NETWORK_HANDLER_THREAD_COUNT);
        Assert.assertEquals(configuration.getMaxAssignedOrdinaryPeerCount(),
                NodeConstants.DEFAULT_MAX_ASSIGNED_ORDINARY_PEER_COUNT);
        Assert.assertEquals(configuration.getMaxUnstructuredPeerCount(),
                NodeConstants.DEFAULT_MAX_UNSTRUCTURED_PEER_COUNT);
        Assert.assertEquals(configuration.getHeartbeatInterval(), NodeConstants.DEFAULT_HEARTBEAT_INTERVAL);
        Assert.assertEquals(configuration.getGossipingInterval(), NodeConstants.DEFAULT_GOSSIPING_INTERVAL);
        Assert.assertEquals(configuration.getNetworkHandlerSendTimeout(),
                NodeConstants.DEFAULT_NETWORK_HANDLER_SEND_TIMEOUT);
        Assert.assertEquals(configuration.getSerSuperPeerTimeout(), NodeConstants.DEFAULT_SER_SUPER_PEER_TIMEOUT);
        Assert.assertEquals(configuration.getNetworkHandlerType(), NodeConstants.DEFAULT_NETWORK_HANDLER);
        Assert.assertEquals(configuration.getRoutingStrategyType(), NodeConstants.DEFAULT_ROUTING_STRATEGY);
    }

    @Test(priority = 2)
    public void testGetConfigurationAtSecondTime() {
        logger.info("Running Service Holder Test 03 - Get configuration at second time");

        Configuration initialConfiguration = serviceHolder.getConfiguration();
        Configuration finalConfiguration = serviceHolder.getConfiguration();

        Assert.assertNotNull(initialConfiguration);
        Assert.assertNotNull(finalConfiguration);
        Assert.assertTrue(initialConfiguration == finalConfiguration);
    }

    @Test(priority = 3)
    public void testGetResourceIndexAtFirstTime() {
        logger.info("Running Service Holder Test 04 - Get resource index at first time");

        ResourceIndex resourceIndex = serviceHolder.getResourceIndex();

        Assert.assertNotNull(resourceIndex);
    }

    @Test(priority = 4)
    public void testGetResourceIndexAtSecondTime() {
        logger.info("Running Service Holder Test 05 - Get resource index at second time");

        ResourceIndex initialResourceIndex = serviceHolder.getResourceIndex();
        ResourceIndex finalResourceIndex = serviceHolder.getResourceIndex();

        Assert.assertNotNull(initialResourceIndex);
        Assert.assertNotNull(finalResourceIndex);
        Assert.assertTrue(initialResourceIndex == finalResourceIndex);
    }

    @Test(priority = 5)
    public void testGetOverlayNetworkManagerAtFirstTime() {
        logger.info("Running Service Holder Test 06 - Get overlay network manager at first time");

        OverlayNetworkManager overlayNetworkManager = serviceHolder.getOverlayNetworkManager();

        Assert.assertNotNull(overlayNetworkManager);

        Object routerInternalState = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertNotNull(routerInternalState);
        Object listenersListInternalState = Whitebox.getInternalState(routerInternalState, "listenersList");
        Assert.assertTrue(listenersListInternalState instanceof List<?>);
        List<?> routerListeners = (List<?>) listenersListInternalState;
        Assert.assertEquals(routerListeners.size(), 1);
        Assert.assertTrue(routerListeners.get(0) == overlayNetworkManager);
    }

    @Test(priority = 6)
    public void testGetOverlayNetworkManagerAtSecondTime() {
        logger.info("Running Service Holder Test 07 - Get overlay network manager at second time");

        OverlayNetworkManager initialOverlayNetworkManager = serviceHolder.getOverlayNetworkManager();
        OverlayNetworkManager finalOverlayNetworkManager = serviceHolder.getOverlayNetworkManager();

        Assert.assertNotNull(initialOverlayNetworkManager);
        Assert.assertNotNull(finalOverlayNetworkManager);
        Assert.assertTrue(initialOverlayNetworkManager == finalOverlayNetworkManager);
    }

    @Test(priority = 7)
    public void testGetQueryManagerAtFirstTime() {
        logger.info("Running Service Holder Test 08 - Get query manager at first time");

        QueryManager queryManager = serviceHolder.getQueryManager();

        Assert.assertNotNull(queryManager);

        Object routerInternalState = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertNotNull(routerInternalState);
        Object listenersListInternalState = Whitebox.getInternalState(routerInternalState, "listenersList");
        Assert.assertTrue(listenersListInternalState instanceof List<?>);
        List<?> routerListeners = (List<?>) listenersListInternalState;
        Assert.assertEquals(routerListeners.size(), 1);
        Assert.assertTrue(routerListeners.get(0) == queryManager);
    }

    @Test(priority = 8)
    public void testGetQueryManagerAtSecondTime() {
        logger.info("Running Service Holder Test 09 - Get query manager at second time");

        QueryManager initialQueryManager = serviceHolder.getQueryManager();
        QueryManager finalQueryManager = serviceHolder.getQueryManager();

        Assert.assertNotNull(initialQueryManager);
        Assert.assertNotNull(finalQueryManager);
        Assert.assertTrue(initialQueryManager == finalQueryManager);
    }

    @Test(priority = 9)
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
        Configuration configuration = new Configuration();
        configuration.setNetworkHandlerType(NetworkHandlerType.UDP_SOCKET);
        serviceHolder.updateConfiguration(configuration);
        NetworkHandler finalNetworkHandler = router.getNetworkHandler();

        Assert.assertNotNull(initialNetworkHandler);
        Assert.assertNotNull(finalNetworkHandler);
        Assert.assertFalse(initialNetworkHandler == finalNetworkHandler);
        Assert.assertTrue(finalNetworkHandler instanceof UDPSocketNetworkHandler);

        initialNetworkHandler.shutdown();
        finalNetworkHandler.shutdown();
        waitFor(1000);
    }

    @Test(priority = 9)
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
        Configuration configuration = new Configuration();
        configuration.setRoutingStrategyType(RoutingStrategyType.UNSTRUCTURED_RANDOM_WALK);
        serviceHolder.updateConfiguration(configuration);
        RoutingStrategy finalRoutingStrategy = router.getRoutingStrategy();

        Assert.assertNotNull(initialRoutingStrategy);
        Assert.assertNotNull(finalRoutingStrategy);
        Assert.assertFalse(initialRoutingStrategy == finalRoutingStrategy);
        Assert.assertTrue(finalRoutingStrategy instanceof UnstructuredRandomWalkRoutingStrategy);
    }
}
