package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.network.rmi.RMINetworkHandler;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.network.RMINetworkHandler class.
 */
public class RMINetworkHandlerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(RMINetworkHandler.class);

    private RMINetworkHandler rmiSocketNetworkHandler1;
    private RMINetworkHandler rmiSocketNetworkHandler2;
    private NetworkHandlerListener rmiSocketNetworkHandler1Listener;
    private NetworkHandlerListener rmiSocketNetworkHandler2Listener;
    private String localhostIP;
    private int peerListeningPort1;
    private int peerListeningPort2;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private int delay;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing RMI Network Handler Test");

        delay = 1000;
        localhostIP = "127.0.0.1";
        peerListeningPort1 = 6756;
        peerListeningPort2 = 7642;

        ServiceHolder serviceHolder1 = new ServiceHolder();
        serviceHolder1.getConfiguration().setPeerListeningPort(peerListeningPort1);
        rmiSocketNetworkHandler1 = Mockito.spy(new RMINetworkHandler(serviceHolder1));
        rmiSocketNetworkHandler1Listener = Mockito.mock(NetworkHandlerListener.class);
        rmiSocketNetworkHandler1.registerListener(rmiSocketNetworkHandler1Listener);
        rmiSocketNetworkHandler1.startListening();
        waitFor(delay);

        serviceHolder.getConfiguration().setPeerListeningPort(peerListeningPort2);
        rmiSocketNetworkHandler2 = Mockito.spy(new RMINetworkHandler(serviceHolder));
        rmiSocketNetworkHandler2Listener = Mockito.mock(NetworkHandlerListener.class);
        rmiSocketNetworkHandler2.registerListener(rmiSocketNetworkHandler2Listener);
        rmiSocketNetworkHandler2.startListening();
        waitFor(delay);

        message1 = Message.parse("0049 " + MessageType.SER.getValue() + " 127.0.0.1 "
                + peerListeningPort1 + " \"Lord of the Rings\" "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));
        message2 = Message.parse("0049 " + MessageType.SER.getValue() + " 127.0.0.1 "
                + peerListeningPort1 + " Cars "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));
        message3 = Message.parse("0049 " + MessageType.SER.getValue() + " 127.0.0.1 "
                + peerListeningPort1 + " \"Iron Man\" "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));
        message4 = Message.parse("0049 " + MessageType.SER.getValue() + " 127.0.0.1 "
                + peerListeningPort1 + " \"Iron Man 2\" "
                + Integer.toString(NodeConstants.DEFAULT_TIME_TO_LIVE - 1));
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up RMI Network Handler Test");

        rmiSocketNetworkHandler1.shutdown();
        rmiSocketNetworkHandler2.shutdown();
        waitFor(delay);
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running RMI Network Handler Test 01 - Get name");

        Assert.assertNotNull(rmiSocketNetworkHandler1.getName());
    }

    @Test(priority = 2)
    public void testCommunication() {
        logger.info("Running RMI Network Handler Test 02 - Communication");

        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1);
        waitFor(delay);

        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }

    @Test(priority = 3)
    public void testRepeatedCommunication() {
        logger.info("Running RMI Network Handler Test 03 - Repeated communication");

        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1);
        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message2);
        rmiSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message3);
        rmiSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message4);
        waitFor(delay);

        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(rmiSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        Mockito.verify(rmiSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message4));
    }

    @Test(priority = 4)
    public void testShutdown() {
        logger.info("Running RMI Network Handler Test 04 - Shutdown");

        rmiSocketNetworkHandler2.shutdown();
        waitFor(delay);
        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1);
        waitFor(delay);

        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(0))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(rmiSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageSendFailed(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }

    @Test(priority = 5)
    public void testRestart() {
        logger.info("Running RMI Network Handler Test 05 - Restart");

        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1);
        waitFor(delay);

        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));

        serviceHolder.getConfiguration().setPeerListeningPort(8756);
        rmiSocketNetworkHandler2.restart();
        waitFor(delay);
        rmiSocketNetworkHandler1.sendMessage(localhostIP, 8756, message2);
        rmiSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message3);
        waitFor(delay);

        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(rmiSocketNetworkHandler2Listener, Mockito.times(0))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        Mockito.verify(rmiSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageSendFailed(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
    }
}
