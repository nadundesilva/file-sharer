package org.microfuse.file.sharer.node.core.communication.network.rmi;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * A Java RMI based network handler.
 * <p>
 * Uses Java Remte Method Invocation to communicate with other nodes.
 */
public class RMINetworkHandler extends NetworkHandler implements RMINetworkHandlerRemote {
    private static final Logger logger = LoggerFactory.getLogger(RMINetworkHandler.class);

    private Registry registry;
    private String rmiRegistryEntry;

    public RMINetworkHandler(ServiceHolder serviceHolder) {
        super(serviceHolder);
    }

    @Override
    public String getName() {
        return NetworkHandlerType.RMI.getValue();
    }

    @Override
    public void startListening() {
        if (!running) {
            super.startListening();
            int port = serviceHolder.getConfiguration().getPeerListeningPort();
            logger.info("Starting listening at port " + port);
            try {
                // Remove this from the registry if this had been already registered
                removeNetworkHandlerFromRMIService();

                // Starting the RMI registry. Fails if it is already running.
                try {
                    LocateRegistry.createRegistry(Constants.RMI_REGISTRY_PORT);
                } catch (RemoteException e) {
                    logger.warn("RMI registry already exists at port "
                            + serviceHolder.getConfiguration().getPeerListeningPort(), e);
                }

                // Retrieving reference to RMI registry
                registry = LocateRegistry.getRegistry(
                        Constants.LOCALHOST,
                        Constants.RMI_REGISTRY_PORT
                );

                // Rebinding this object in the RMI registry
                Remote remote = UnicastRemoteObject.exportObject(this, port);
                rmiRegistryEntry = getRMIRegistryEntry(
                        serviceHolder.getConfiguration().getIp(),
                        serviceHolder.getConfiguration().getPeerListeningPort()
                );
                registry.rebind(rmiRegistryEntry, remote);

                logger.info("Bind RMI registry item " + rmiRegistryEntry
                        + " with object from class " + this.getClass());
            } catch (RemoteException e) {
                logger.warn("Failed to startInThread listening at port " + port, e);
            }
        } else {
            logger.warn("The RMI network handler is already listening. Ignored request to startInThread again.");
        }
    }

    @Override
    public void restart() {
        if (running) {
            super.restart();
            restartRequired = true;
            try {
                shutdown();
                startListening();
            } finally {
                restartRequired = false;
            }
        } else {
            logger.warn("The RMI network handler is not listening. Ignored request to restart.");
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down RMI network handler");
        running = false;
        removeNetworkHandlerFromRMIService();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        try {
            Registry receiverRegistry = LocateRegistry.getRegistry(ip, Constants.RMI_REGISTRY_PORT);
            String remoteRmiRegistryEntry = getRMIRegistryEntry(ip, port);
            RMINetworkHandlerRemote receiverRMINetworkHandler =
                    (RMINetworkHandlerRemote) receiverRegistry.lookup(remoteRmiRegistryEntry);
            receiverRMINetworkHandler.receiveMessage(
                    serviceHolder.getConfiguration().getIp(),
                    serviceHolder.getConfiguration().getPeerListeningPort(),
                    message.toString()
            );
            logger.info("Message " + message.toString() + " sent to node " + ip + ":" + port);
        } catch (RemoteException | NotBoundException e) {
            logger.warn("Failed to send message " + message.toString() + " to node " + ip + ":" + port, e);
            runTasksOnMessageSendFailed(ip, port, message);
        }
    }

    @Override
    public void receiveMessage(String ip, int port, String messageString) {
        runTasksOnMessageReceived(ip, port, Message.parse(messageString));
    }

    /**
     * Remove the served RMI objects.
     */
    private void removeNetworkHandlerFromRMIService() {
        try {
            while (UnicastRemoteObject.unexportObject(this, false)) { }
            logger.info("Un-exported object");
        } catch (NoSuchObjectException e) {
            logger.warn("Failed to un-export object", e);
        }
    }

    /**
     * Get the RMI network handler registry entry based on ip and port.
     *
     * @param ip   The IP of the peer containing the registry
     * @param port The port of the peer containing the registry
     * @return The RMI registry entry
     */
    private String getRMIRegistryEntry(String ip, int port) {
        return serviceHolder.getConfiguration().getRmiRegistryEntryPrefix() + ip + ":" + port
                + Constants.RMI_REGISTRY_ENTRY_NETWORK_HANDLER;
    }
}
