package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Overlay network related end point.
 */
@Path("/network")
public class OverlayNetworkEndPoint {
    @GET
    @Path("/")
    public Response getInfoAboutSelf() {
        Status responseStatus = Status.SUCCESS;
        Map<String, Object> data = new HashMap<>();

        PeerType peerType = FileSharerHolder.getFileSharer().getServiceHolder().getPeerType();
        data.put(APIConstants.PEER_TYPE, peerType.getValue());

        RoutingTable routingTable = FileSharerHolder.getFileSharer().getServiceHolder().getRouter().getRoutingTable();
        data.put(APIConstants.UNSTRUCTURED_NETWORK, routingTable.getAllUnstructuredNetworkRoutingTableNodes());
        if (peerType == PeerType.SUPER_PEER) {
            if (routingTable instanceof SuperPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
                data.put(APIConstants.SUPER_PEER_NETWORK,
                        superPeerRoutingTable.getAllSuperPeerNetworkRoutingTableNodes());
                data.put(APIConstants.ASSIGNED_ORDINARY_PEERS,
                        superPeerRoutingTable.getAllAssignedOrdinaryNetworkRoutingTableNodes());
            } else {
                responseStatus = Status.ERROR;
            }
        } else {
            if (routingTable instanceof OrdinaryPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
                data.put(APIConstants.ASSIGNED_SUPER_PEER, ordinaryPeerRoutingTable.getAssignedSuperPeer());
            } else {
                responseStatus = Status.ERROR;
            }
        }

        Map<String, Object> response = ResponseUtils.generateCustomResponse(responseStatus);
        response.put(APIConstants.DATA, data);
        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
