package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageConstants;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.network.BootstrapServerNetworkHandler class.
 */
public class BootstrapServerNetworkHandlerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapServerNetworkHandlerTestCase.class);

    private BootstrapServerNetworkHandler bootstrapServerNetworkHandler;
    private NetworkHandlerListener udpSocketNetworkHandlerListener;
    private String localhostIP;
    private int delay;
    private BootstrapServer bootstrapServer;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing UDP Network Handler Test");

        delay = 1000;
        localhostIP = "127.0.0.1";

        bootstrapServer = new BootstrapServer();
        bootstrapServer.start();
        waitFor(delay);

        bootstrapServerNetworkHandler = Mockito.spy(new BootstrapServerNetworkHandler(serviceHolder));
        udpSocketNetworkHandlerListener = Mockito.mock(NetworkHandlerListener.class);
        bootstrapServerNetworkHandler.registerListener(udpSocketNetworkHandlerListener);
        bootstrapServerNetworkHandler.startListening();
        waitFor(delay);
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up UDP Network Handler Test");

        bootstrapServerNetworkHandler.shutdown();
        waitFor(delay);

        bootstrapServer.shutdown();
        waitFor(delay);
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running UDP Network Handler Test 01 - Get name");

        Assert.assertNotNull(bootstrapServerNetworkHandler.getName());
    }

    @Test(priority = 2)
    public void testEcho() {
        logger.info("Running UDP Network Handler Test 02 - Echo to bootstrap server");

        Message echoMessage = new Message();
        echoMessage.setType(MessageType.ECHO);

        Message echoOkMessage = new Message();
        echoOkMessage.setType(MessageType.ECHO_OK);
        echoOkMessage.setData(MessageIndexes.ECHO_OK_VALUE, MessageConstants.ECHO_OK_VALUE_SUCCESS);

        NetworkHandler networkHandler = Mockito.mock(NetworkHandler.class);
        RoutingStrategy routingStrategy = Mockito.mock(RoutingStrategy.class);
        Router router = Mockito.spy(new Router(networkHandler, routingStrategy, serviceHolder));

        Object routerInternalState = Whitebox.getInternalState(router, "bootstrapServerNetworkHandler");
        Assert.assertTrue(routerInternalState instanceof BootstrapServerNetworkHandler);
        BootstrapServerNetworkHandler bootstrapServerNetworkHandler =
                (BootstrapServerNetworkHandler) routerInternalState;
        bootstrapServerNetworkHandler.clearListeners();
        bootstrapServerNetworkHandler.registerListener(router);

        router.sendMessageToBootstrapServer(echoMessage);
        waitFor(delay);

        Mockito.verify(router, Mockito.times(1)).onMessageReceived(
                serviceHolder.getConfiguration().getBootstrapServerIP(),
                serviceHolder.getConfiguration().getBootstrapServerPort(),
                echoOkMessage
        );

        bootstrapServer.shutdown();
    }
}
