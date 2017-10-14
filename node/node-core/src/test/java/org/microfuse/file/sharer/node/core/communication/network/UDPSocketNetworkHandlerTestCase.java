package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.network.UDPSocketNetworkHandler class.
 */
public class UDPSocketNetworkHandlerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    private UDPSocketNetworkHandler udpSocketNetworkHandler1;
    private UDPSocketNetworkHandler udpSocketNetworkHandler2;
    private NetworkHandlerListener udpSocketNetworkHandler1Listener;
    private NetworkHandlerListener udpSocketNetworkHandler2Listener;
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
        delay = 1000;
        localhostIP = "127.0.0.1";
        peerListeningPort1 = 6756;
        peerListeningPort2 = 7642;
        Configuration configuration = ServiceHolder.getConfiguration();

        configuration.setPeerListeningPort(peerListeningPort1);
        udpSocketNetworkHandler1 = Mockito.spy(new UDPSocketNetworkHandler());
        udpSocketNetworkHandler1Listener = Mockito.mock(NetworkHandlerListener.class);
        udpSocketNetworkHandler1.registerListener(udpSocketNetworkHandler1Listener);
        udpSocketNetworkHandler1.startListening();
        waitFor(delay);

        configuration.setPeerListeningPort(peerListeningPort2);
        udpSocketNetworkHandler2 = Mockito.spy(new UDPSocketNetworkHandler());
        udpSocketNetworkHandler2Listener = Mockito.mock(NetworkHandlerListener.class);
        udpSocketNetworkHandler2.registerListener(udpSocketNetworkHandler2Listener);
        udpSocketNetworkHandler2.startListening();
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
        udpSocketNetworkHandler1.shutdown();
        udpSocketNetworkHandler2.shutdown();
        waitFor(delay);
    }

    @Test
    public void testName() {
        Assert.assertNotNull(udpSocketNetworkHandler1.getName());
    }

    @Test
    public void testCommunication() {
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }

    @Test
    public void testRepeatedCommunication() {
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        udpSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message3, false);
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message2, false);
        udpSocketNetworkHandler2.sendMessage(localhostIP, peerListeningPort1, message4, false);
        waitFor(delay);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(udpSocketNetworkHandler1Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        Mockito.verify(udpSocketNetworkHandler1Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message4));
    }

    @Test
    public void testCommunicationWithWaiting() {
        Mockito.doAnswer(invocationOnMock -> {
            Object[] arguments = invocationOnMock.getArguments();

            Assert.assertTrue(arguments[0] instanceof String);
            String ip = (String) arguments[0];
            Assert.assertTrue(arguments[1] instanceof Integer);
            int port = (int) arguments[1];

            udpSocketNetworkHandler2.sendMessage(ip, port, message2, false);
            return null;
        }).when(udpSocketNetworkHandler2Listener)
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));

        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, true);
        waitFor(delay * 2);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        Mockito.verify(udpSocketNetworkHandler1Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
    }

    @Test
    public void testRestart() {
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1))
                .onMessageReceived(Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));

        ServiceHolder.getConfiguration().setPeerListeningPort(8756);
        udpSocketNetworkHandler2.restart();
        waitFor(delay);
        udpSocketNetworkHandler1.sendMessage(localhostIP, 8756, message2, false);
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message3, false);
        waitFor(delay);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(1)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message2));
        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(0)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
        // TODO : uncomment after implementing reliability in UDP
//        Mockito.verify(udpSocketNetworkHandler1Listener, Mockito.times(1)).onMessageSendFailed(
//                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message3));
    }

    @Test
    public void testShutdown() {
        udpSocketNetworkHandler2.shutdown();
        udpSocketNetworkHandler1.sendMessage(localhostIP, peerListeningPort2, message1, false);
        waitFor(delay);

        Mockito.verify(udpSocketNetworkHandler2Listener, Mockito.times(0)).onMessageReceived(
                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
        // TODO : uncomment after implementing reliability in UDP
//        Mockito.verify(udpSocketNetworkHandler1Listener, Mockito.times(1)).onMessageSendFailed(
//                Mockito.eq(localhostIP), Mockito.anyInt(), Mockito.eq(message1));
    }
}
