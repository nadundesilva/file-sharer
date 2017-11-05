package org.microfuse.file.sharer.node.core.communication.network.rmi;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
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
    private Remote remote;
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
            logger.debug("Starting listening at port " + port);
            try {
                closeRMIService();
                try {
                    LocateRegistry.createRegistry(Constants.RMI_REGISTRY_PORT);
                } catch (RemoteException e) {
                    logger.warn("RMI registry already exists at port "
                            + serviceHolder.getConfiguration().getPeerListeningPort(), e);
                }
                registry = LocateRegistry.getRegistry(
                        serviceHolder.getConfiguration().getIp(),
                        Constants.RMI_REGISTRY_PORT
                );
                try {
                    remote = UnicastRemoteObject.exportObject(this, port);
                } catch (RemoteException e) {
                    logger.warn("Failed to export remote", e);
                }
                rmiRegistryEntry = getRMIRegistryEntry(
                        serviceHolder.getConfiguration().getIp(),
                        serviceHolder.getConfiguration().getPeerListeningPort()
                );
                registry.rebind(rmiRegistryEntry, remote);
                logger.debug("Bind RMI registry item " + rmiRegistryEntry
                        + " with object from class " + this.getClass());
            } catch (RemoteException e) {
                logger.warn("Failed to start listening at port " + port, e);
            }
        } else {
            logger.warn("The RMI network handler is already listening. Ignored request to start again.");
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
        logger.debug("Shutting down RMI network handler");
        running = false;
        closeRMIService();
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
            logger.debug("Message " + message.toString() + " sent to node " + ip + ":" + port);
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
    private void closeRMIService() {
        try {
            while (UnicastRemoteObject.unexportObject(this, false)) { }
            logger.debug("Un-exported object");
        } catch (NoSuchObjectException e) {
            logger.warn("Failed to un-export object", e);
        }
        if (registry != null) {
            try {
                registry.unbind(rmiRegistryEntry);
                logger.debug("Unbind RMI registry item " + rmiRegistryEntry);
            } catch (RemoteException | NotBoundException e) {
                logger.warn("Failed to stop listening", e);
            }
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
