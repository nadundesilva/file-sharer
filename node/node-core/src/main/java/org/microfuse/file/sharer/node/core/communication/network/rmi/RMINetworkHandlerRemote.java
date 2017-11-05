package org.microfuse.file.sharer.node.core.communication.network.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface for RMI network handler.
 */
public interface RMINetworkHandlerRemote extends Remote {
    /**
     * Receive method used by RMI calls.
     *
     * @param ip            The ip of the sending node
     * @param port          The port of the sending node
     * @param messageString The message to be sent
     */
    void receiveMessage(String ip, int port, String messageString) throws RemoteException;
}
