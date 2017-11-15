package org.microfuse.file.sharer.node.core.tracing;

import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Tracer interface.
 * This is a RMI stub interface.
 */
public interface Tracer extends Remote {
    /**
     * Register node in the tracer.
     *
     * @param timeStamp           The timestamp at which the operation occurred
     * @param ip                  The ip of the traceable node
     * @param port                The port of the traceable node
     * @param currentRoutingTable The current routing table in the traceable node
     */
    void register(long timeStamp, String ip, int port, RoutingTable currentRoutingTable)
            throws RemoteException;

    /**
     * Add a unstructured network connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void addUnstructuredNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Add a super peer network connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void addSuperPeerNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Add a assigned ordinary peer connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void addAssignedOrdinaryPeerConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Remove a unstructured network connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void removeUnstructuredNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Remove a super peer network connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void removeSuperPeerNetworkConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Remove a assigned ordinary peer connection.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip1       The ip of node1
     * @param port1     The port of node1
     * @param ip2       The ip of node2
     * @param port2     The port of node2
     */
    void removeAssignedOrdinaryPeerConnection(long timeStamp, String ip1, int port1, String ip2, int port2)
            throws RemoteException;

    /**
     * Promote a node to a super peer.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip        The ip of the node to be promoted
     * @param port      The port of the node to be promoted
     */
    void promoteToSuperPeer(long timeStamp, String ip, int port) throws RemoteException;

    /**
     * Demote a node to a ordinary peer.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip        The ip of the node to be demoted
     * @param port      The port of the node to be demoted
     */
    void demoteToOrdinaryPeer(long timeStamp, String ip, int port) throws RemoteException;

    /**
     * Demote a node to a ordinary peer.
     *
     * @param timeStamp The timestamp at which the operation occurred
     * @param ip        The ip of the node to be demoted
     * @param port      The port of the node to be demoted
     * @param message   The message sent
     */
    void notifyMessageSend(long timeStamp, String ip, int port, Message message) throws RemoteException;
}
