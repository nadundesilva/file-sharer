package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.Manager;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class SocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(SocketNetworkHandler.class);

    @Override
    public String getName() {
        return NetworkHandlerType.SOCKET.getValue();
    }

    @Override
    public void startListening() {
        int portNumber = Manager.getConfigurationInstance().getTcpListeningPort();
        new Thread(() -> {
            while (true) {
                try (
                        ServerSocket serverSocket = new ServerSocket(portNumber);
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                Constants.DEFAULT_CHARSET))
                ) {
                    new Thread(() -> {
                        try {
                            StringBuilder message = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                message.append(inputLine);
                            }
                            onMessageReceived(clientSocket.getRemoteSocketAddress().toString(), message.toString());
                        } catch (IOException e) {
                            logger.debug("Failed to receive message from "
                                    + clientSocket.getRemoteSocketAddress().toString(), e);
                        }
                    }).start();
                } catch (IOException e) {
                    logger.debug("Failed to establish socket connection", e);
                }
            }
        }).start();
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
            logger.debug("Message sent to " + ip + ":" + port + " : " + message);
        }
    }
}
