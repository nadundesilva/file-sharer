package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.core.tracing.Traceable;
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
 * Trace Manager.
 */
public class TraceManager implements Traceable {
    private static final Logger logger = LoggerFactory.getLogger(TraceManager.class);

    private ServiceHolder serviceHolder;

    private Registry registry;

    public TraceManager(ServiceHolder serviceHolder) {
        this.serviceHolder = serviceHolder;

        // Starting the RMI registry. Fails if it is already running.
        try {
            LocateRegistry.createRegistry(Constants.RMI_REGISTRY_PORT);
        } catch (RemoteException e) {
            logger.warn("Failed to start RMI registry since it already exists at port "
                    + Constants.RMI_REGISTRY_PORT, e);
        }

        // Retrieving reference to RMI registry
        try {
            registry = LocateRegistry.getRegistry(Constants.LOCALHOST, Constants.RMI_REGISTRY_PORT);
        } catch (RemoteException e) {
            logger.warn("Failed to locate registry", e);
        }
    }

    /**
     * Start listening to tracer.
     */
    public void start() {
        System.setProperty(Constants.RMI_HOME_SYSTEM_PROPERTY, serviceHolder.getConfiguration().getIp());

        int port = serviceHolder.getConfiguration().getPeerListeningPort();
        try {
            // Rebinding this object in the RMI registry
            String rmiRegistryEntry = getRMIRegistryEntry(
                    serviceHolder.getConfiguration().getRmiRegistryEntryPrefix(),
                    serviceHolder.getConfiguration().getIp(),
                    serviceHolder.getConfiguration().getPeerListeningPort()
            );
            Remote remote = UnicastRemoteObject.exportObject(this, port);
            registry.rebind(rmiRegistryEntry, remote);

            logger.info("Bind RMI registry item " + rmiRegistryEntry
                    + " with object from class " + this.getClass());
        } catch (RemoteException e) {
            logger.warn("Failed to startInThread listening at port " + port, e);
        }
        logger.info("Started listening at port " + port);
    }

    /**
     * Stop listening to the tracer.
     */
    public void shutdown() {
        logger.info("Shutting down trace manager");

        // Removing this object from the RMI registry
        try {
            registry.unbind(getRMIRegistryEntry(
                    serviceHolder.getConfiguration().getRmiRegistryEntryPrefix(),
                    serviceHolder.getConfiguration().getIp(),
                    serviceHolder.getConfiguration().getPeerListeningPort()
            ));
        } catch (RemoteException | NotBoundException e) {
            logger.warn("Failed to unbind object from the registry", e);
        }

        // Un-exporting this object
        try {
            while (UnicastRemoteObject.unexportObject(this, false)) { }
            logger.info("Un-exported object");
        } catch (NoSuchObjectException e) {
            logger.warn("Failed to un-export object", e);
        }
    }

    /**
     * Get the Trace Manager registry entry based on ip and port.
     *
     * @param prefix The RMI registry entry prefix
     * @param ip     The IP of the peer containing the registry
     * @param port   The port of the peer containing the registry
     * @return The RMI registry entry
     */
    public static String getRMIRegistryEntry(String prefix, String ip, int port) {
        return prefix + ip + ":" + port + Constants.RMI_REGISTRY_ENTRY_TRACEABLE_POSTFIX;
    }

    @Override
    public boolean heartbeat() throws RemoteException {
        return true;
    }
}
