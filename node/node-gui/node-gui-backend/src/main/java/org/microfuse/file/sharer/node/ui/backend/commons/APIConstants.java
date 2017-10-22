package org.microfuse.file.sharer.node.ui.backend.commons;

/**
 * Constants related to the request sent and responses sent by the API.
 */
public class APIConstants {
    public static final String STATUS = "status";
    public static final String DATA = "data";

    public static final String PEER_TYPE = "peerType";
    public static final String UNSTRUCTURED_NETWORK = "unstructuredNetwork";
    public static final String SUPER_PEER_NETWORK = "superPeerNetwork";
    public static final String ASSIGNED_ORDINARY_PEERS = "assignedOrdinaryPeers";
    public static final String ASSIGNED_SUPER_PEER = "assignedSuperPeer";

    private APIConstants() {   // Preventing from being initiated
    }
}
