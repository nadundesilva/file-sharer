package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.messaging.TCPMessage;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * A TCP Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class TCPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    private ServerSocket serverSocket;

    public TCPSocketNetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        if (!running) {
            super.startListening();
            Thread thread = new Thread(() -> {
                while (running) {
                    int port = serviceHolder.getConfiguration().getPeerListeningPort();
                    Socket clientSocket = null;
                    BufferedReader in = null;
                    try {
                        serverSocket = new ServerSocket(port);
                        serverSocket.setReuseAddress(false);

                        logger.info("Started listening at " + port + ".");
                        while (running && !restartRequired) {
                            clientSocket = serverSocket.accept();
                            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                    Constants.DEFAULT_CHARSET));

                            StringBuilder message = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                message.append(inputLine);
                            }

                            TCPMessage tcpMessage = TCPMessage.parse(message.toString());
                            runTasksOnMessageReceived(
                                    tcpMessage.getSourceIP(), tcpMessage.getSourcePort(), tcpMessage.getMessage()
                            );
                        }
                    } catch (IOException e) {
                        logger.info("Listening stopped", e);
                    } finally {
                        Closeables.closeQuietly(in);
                        try {
                            Closeables.close(clientSocket, true);
                        } catch (IOException ignored) {
                        }
                        closeSocket();
                    }
                }
            });
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        } else {
            logger.warn("The TCP network handler is already listening. Ignored request to start again.");
        }
    }

    @Override
    public void restart() {
        if (running) {
            super.restart();
            restartRequired = true;
            try {
                closeSocket();
            } finally {
                restartRequired = false;
            }
        } else {
            logger.warn("The TCP network handler is not listening. Ignored request to restart.");
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down TCP network handler");
        running = false;
        closeSocket();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        TCPMessage tcpMessage = new TCPMessage();
        tcpMessage.setSourceIP(serviceHolder.getConfiguration().getIp());
        tcpMessage.setSourcePort(serviceHolder.getConfiguration().getPeerListeningPort());
        tcpMessage.setMessage(message);

        try (
                Socket sendSocket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream(),
                        Constants.DEFAULT_CHARSET), true)

        ) {
            out.write(tcpMessage.toString());
            logger.info("Message " + tcpMessage.toString() + " sent to node " + ip + ":" + port);
        } catch (IOException e) {
            logger.info("Failed to send message " + tcpMessage.toString() + " to " + ip + ":" + port, e);
            runTasksOnMessageSendFailed(ip, port, tcpMessage.getMessage());
        }
    }

    /**
     * Close the TCP socket.
     */
    private void closeSocket() {
        // Set reusable to enable a new network handler to use the same port
        if (serverSocket != null) {
            try {
                serverSocket.setReuseAddress(true);
            } catch (SocketException e) {
                logger.warn("Failed to set socket to reusable", e);
            }
        }

        try {
            Closeables.close(serverSocket, true);
        } catch (IOException ignored) {
        }
    }
}
