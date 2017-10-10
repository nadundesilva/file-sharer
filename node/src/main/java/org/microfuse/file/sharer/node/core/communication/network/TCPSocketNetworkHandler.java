package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.Constants;
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
 * A Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class TCPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    private ServerSocket serverSocket;

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        super.startListening();
        new Thread(() -> {
            while (running) {
                int portNumber = ServiceHolder.getConfiguration().getPeerListeningPort();
                BufferedReader in = null;
                try {
                    serverSocket = new ServerSocket(portNumber);
                    serverSocket.setReuseAddress(false);
                    logger.debug("Started listening at " + portNumber + ".");
                    while (running && !restartRequired) {
                        Socket clientSocket = serverSocket.accept();
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                Constants.DEFAULT_CHARSET));

                        StringBuilder message = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            message.append(inputLine);
                        }
                        onMessageReceived(
                                clientSocket.getInetAddress().getHostAddress(),
                                clientSocket.getPort(),
                                Message.parse(message.toString())
                        );
                    }
                    logger.debug("Restarted socket");
                } catch (IOException e) {
                    logger.debug("Failed to establish socket connection", e);
                } finally {
                    Closeables.closeQuietly(in);
                    closeSocket();
                }
            }
        }).start();
    }

    @Override
    public void restart() {
        super.restart();
        restartRequired = true;
        closeSocket();
        restartRequired = false;
    }

    @Override
    public void shutdown() {
        running = false;
        closeSocket();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        try (
                Socket echoSocket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(echoSocket.getOutputStream(),
                        Constants.DEFAULT_CHARSET), true)
        ) {
            out.write(message.toString());
        } catch (IOException e) {
            logger.debug("Message sent to " + ip + ":" + port + " : " + message, e);
            onMessageSendFailed(ip, port, message);
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
        } catch (IOException e) {
            logger.warn("Failed to shutdown socket", e);
        }
    }
}
