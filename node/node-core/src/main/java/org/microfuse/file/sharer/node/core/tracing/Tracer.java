package org.microfuse.file.sharer.node.core.tracing;

import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.rmi.Remote;

/**
 * Tracer interface.
 * This is a RMI stub interface.
 */
public interface Tracer extends Remote {
    /**
     * Register node in the tracer.
     *
     * @param ip                  The ip of the traceable node
     * @param port                The port of the traceable node
     * @param currentRoutingTable The current routing table in the traceable node
     */
    void register(String ip, int port, RoutingTable currentRoutingTable);

    /**
     * Add a unstructured network connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void addUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2);

    /**
     * Add a super peer network connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void addSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2);

    /**
     * Add a assigned ordinary peer connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void addAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2);

    /**
     * Remove a unstructured network connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void removeUnstructuredNetworkConnection(String ip1, int port1, String ip2, int port2);

    /**
     * Remove a super peer network connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void removeSuperPeerNetworkConnection(String ip1, int port1, String ip2, int port2);

    /**
     * Remove a assigned ordinary peer connection.
     *
     * @param ip1    The ip of node1
     * @param port1  The port of node1
     * @param ip2    The ip of node2
     * @param port2  The port of node2
     */
    void removeAssignedOrdinaryPeerConnection(String ip1, int port1, String ip2, int port2);
}
