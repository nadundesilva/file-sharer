package org.microfuse.file.sharer.node.core.tracing;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Traceable interface.
 * This is a RMI stub interface.
 */
public interface Traceable extends Remote {
    /**
     * Heartbeats to the node.
     *
     * @return True if the node is active. False if inactive
     * @throws RemoteException if the heartbeat cannot connect to the node
     */
    boolean heartbeat() throws RemoteException;
}
