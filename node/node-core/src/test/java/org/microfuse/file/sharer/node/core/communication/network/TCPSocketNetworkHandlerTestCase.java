package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.network.TCPSocketNetworkHandler class.
 */
public class TCPSocketNetworkHandlerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    private TCPSocketNetworkHandler tcpSocketNetworkHandler1;
    private TCPSocketNetworkHandler tcpSocketNetworkHandler2;
    private NetworkHandlerListener tcpSocketNetworkHandler1Listener;
    private NetworkHandlerListener tcpSocketNetworkHandler2Listener;
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
        logger.info("Initializing TCP Network Handler Test");

        delay = 1000;
        localhostIP = "127.0.0.1";
        peerListeningPort1 = 6756;
        peerListeningPort2 = 7642;
        Configuration configuration = serviceHolder.getConfiguration();

        configuration.setPeerListeningPort(peerListeningPort1);
        tcpSocketNetworkHandler1 = Mockito.spy(new TCPSocketNetworkHandler(serviceHolder));
        tcpSocketNetworkHandler1Listener = Mockito.mock(NetworkHandlerListener.class);
        tcpSocketNetworkHandler1.registerListener(tcpSocketNetworkHandler1Listener);
        tcpSocketNetworkHandler1.startListening();
        waitFor(delay);

        configuration.setPeerListeningPort(peerListeningPort2);
        tcpSocketNetworkHandler2 = Mockito.spy(new TCPSocketNetworkHandler(serviceHolder));
        tcpSocketNetworkHandler2Listener = Mockito.mock(NetworkHandlerListener.class);
        tcpSocketNetworkHandler2.registerListener(tcpSocketNetworkHandler2Listener);
        tcpSocketNetworkHandler2.startListening();
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
        logger.info("Cleaning Up TCP Network Handler Test");

        tcpSocketNetworkHandler1.shutdown();
        tcpSocketNetworkHandler2.shutdown();
        waitFor(delay);
    }

    @Test
    public void testName() {
        logger.info("Running TCP Network Handler Test 01 - Get name");

        Assert.assertNotNull(tcpSocketNetworkHandler1.getName());
    }

    @Test
    public void testCommunication() {
        logger.info("Running TCP Network Handler Test 02 - Communication");

        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }

    @Test
    public void testRepeatedCommunication() {
        logger.info("Running TCP Network Handler Test 03 - Repeated communication");

        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message2, false);
        tcpSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message3, false);
        tcpSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message4, false);
        waitFor(delay);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(tcpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        Mockito.verify(tcpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message4));
    }

    @Test
    public void testCommunicationWithWaiting() {
        logger.info("Running TCP Network Handler Test 04 - Communication with waiting");

        Mockito.doAnswer(invocationOnMock -> {
            Object[] arguments = invocationOnMock.getArguments();

            Assert.assertTrue(arguments[0] instanceof String);
            String ip = (String) arguments[0];
            Assert.assertTrue(arguments[1] instanceof Integer);
            int port = (int) arguments[1];

            tcpSocketNetworkHandler2.sendMessage(ip, port, message2, false);
            return null;
        }).when(tcpSocketNetworkHandler2Listener)
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));

        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, true);
        waitFor(delay * 2);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(tcpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
    }

    @Test
    public void testRestart() {
        logger.info("Running TCP Network Handler Test 05 - Restart");

        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));

        serviceHolder.getConfiguration().setPeerListeningPort(8756);
        tcpSocketNetworkHandler2.restart();
        waitFor(delay);
        tcpSocketNetworkHandler1.sendMessage(localhostIP, 8756, message2, false);
        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message3, false);
        waitFor(delay);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(0))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        Mockito.verify(tcpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageSendFailed(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
    }

    @Test
    public void testShutdown() {
        logger.info("Running TCP Network Handler Test 06 - Shutdown");

        tcpSocketNetworkHandler2.shutdown();
        tcpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(tcpSocketNetworkHandler2Listener, Mockito.times(0))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(tcpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageSendFailed(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }
}
