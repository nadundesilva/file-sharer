package org.microfuse.file.sharer.node.commons.tracing;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;

/**
 * Node with tracing added tracing information.
 */
public class TraceableNode extends Node {
    private static final long serialVersionUID = 105L;
    private PeerType peerType;

    public TraceableNode(String ip, int port) {
        super(ip, port);
        peerType = PeerType.ORDINARY_PEER;
    }

    public PeerType getPeerType() {
        return peerType;
    }

    public void setPeerType(PeerType peerType) {
        this.peerType = peerType;
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
