package org.microfuse.file.sharer.bootstrap;

import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Bootstrap Server which can act as the main server to which every node can connect to.
 * The server will provide ip's to which the nodes can connect to.
 */
public class BootstrapServer {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapServer.class);

    public static void main(String args[]) {
        DatagramSocket sock;
        String s;
        List<Neighbour> nodes = new ArrayList<>();

        try {
            sock = new DatagramSocket(Constants.BOOTSTRAP_SERVER_LISTENER_PORT);

            echo("Bootstrap Server created at " + Constants.BOOTSTRAP_SERVER_LISTENER_PORT
                    + ". Waiting for incoming data...");

            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                sock.receive(incoming);

                byte[] data = incoming.getData();
                s = new String(data, 0, incoming.getLength(), Constants.DEFAULT_CHARSET);

                //echo the details of incoming data - client ip : client port - client message
                echo(incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - " + s);

                StringTokenizer st = new StringTokenizer(s, " ");
                String command = st.nextToken();

                if (Objects.equals(command, MessageType.REG.getValue())) {
                    StringBuilder reply = new StringBuilder(MessageType.REG_OK.getValue() + " ");

                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    String username = st.nextToken();
                    if (nodes.size() == 0) {
                        reply.append("0");
                        nodes.add(new Neighbour(ip, port, username));
                    } else {
                        boolean isOkay = true;
                        for (Neighbour node : nodes) {
                            if (node.getPort() == port) {
                                if (node.getUsername().equals(username)) {
                                    reply.append("9998");
                                } else {
                                    reply.append("9997");
                                }
                                isOkay = false;
                            }
                        }
                        if (isOkay) {
                            if (nodes.size() == 1) {
                                reply.append("1 ")
                                        .append(nodes.get(0).getIp())
                                        .append(" ")
                                        .append(nodes.get(0).getPort());
                            } else if (nodes.size() == 2) {
                                reply.append("2 ")
                                        .append(nodes.get(0).getIp())
                                        .append(" ")
                                        .append(nodes.get(0).getPort()).append(" ")
                                        .append(nodes.get(1).getIp())
                                        .append(" ").append(nodes.get(1).getPort());
                            } else {
                                Random r = new Random();
                                int low = 0;
                                int high = nodes.size();
                                int random1 = r.nextInt(high - low) + low;
                                int random2 = r.nextInt(high - low) + low;
                                while (random1 == random2) {
                                    random2 = r.nextInt(high - low) + low;
                                }
                                echo(random1 + " " + random2);
                                reply.append("2 ")
                                        .append(nodes.get(random1).getIp())
                                        .append(" ")
                                        .append(nodes.get(random1).getPort())
                                        .append(" ")
                                        .append(nodes.get(random2).getIp())
                                        .append(" ")
                                        .append(nodes.get(random2).getPort());
                            }
                            nodes.add(new Neighbour(ip, port, username));
                        }
                    }

                    String stringReply = String.format("%04d", reply.length() + 5) + " " + reply.toString();

                    DatagramPacket dpReply = new DatagramPacket(
                            stringReply.getBytes(Constants.DEFAULT_CHARSET),
                            stringReply.getBytes(Constants.DEFAULT_CHARSET).length,
                            incoming.getAddress(), incoming.getPort());
                    sock.send(dpReply);
                    break;
                } else if (Objects.equals(command, MessageType.UNREG.getValue())) {
                    int port = Integer.parseInt(st.nextToken());
                    for (int i = 0; i < nodes.size(); i++) {
                        if (nodes.get(i).getPort() == port) {
                            nodes.remove(i);
                            String reply = "0012 " + MessageType.UNREG_OK + " 0";
                            DatagramPacket dpReply = new DatagramPacket(
                                    reply.getBytes(Constants.DEFAULT_CHARSET),
                                    reply.getBytes(Constants.DEFAULT_CHARSET).length,
                                    incoming.getAddress(), incoming.getPort());
                            sock.send(dpReply);
                        }
                    }
                    break;
                } else if (Objects.equals(command, MessageType.ECHO.getValue())) {
                    for (Neighbour node : nodes) {
                        echo(node.getIp() + " " + node.getPort()
                                + " " + node.getUsername());
                    }
                    String reply = "0012 " + MessageType.ECHO_OK.getValue() + " 0";
                    DatagramPacket dpReply = new DatagramPacket(
                            reply.getBytes(Constants.DEFAULT_CHARSET),
                            reply.getBytes(Constants.DEFAULT_CHARSET).length, incoming.getAddress(),
                            incoming.getPort());
                    sock.send(dpReply);
                    break;
                }

            }
        } catch (IOException e) {
            logger.error("IOException " + e);
        }
    }

    /**
     * simple function to echo data to terminal.
     *
     * @param message The message to be printed
     */
    private static void echo(String message) {
        logger.info(message);
    }
}
