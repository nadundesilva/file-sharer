package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.commons.peer.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    public Response getInfoAboutSelf() {
        Status responseStatus = Status.SUCCESS;
        Map<String, Object> data = new HashMap<>();

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            PeerType peerType = FileSharerHolder.getFileSharer().getServiceHolder().getPeerType();
            data.put(APIConstants.PEER_TYPE, peerType.toString());

            RoutingTable routingTable =
                    FileSharerHolder.getFileSharer().getServiceHolder().getRouter().getRoutingTable();

            Set<Node> unstructuredNetworkNodes = routingTable.getAllUnstructuredNetworkNodes();
            unstructuredNetworkNodes = unstructuredNetworkNodes.stream().parallel()
                    .filter(Node::isActive)
                    .collect(Collectors.toSet());
            data.put(APIConstants.UNSTRUCTURED_NETWORK, unstructuredNetworkNodes);

            if (peerType == PeerType.SUPER_PEER) {
                if (routingTable instanceof SuperPeerRoutingTable) {
                    SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;

                    Set<Node> superPeerNetworkNodes = superPeerRoutingTable.getAllSuperPeerNetworkNodes();
                    superPeerNetworkNodes = superPeerNetworkNodes.stream().parallel()
                            .filter(Node::isActive)
                            .collect(Collectors.toSet());
                    data.put(APIConstants.SUPER_PEER_NETWORK, superPeerNetworkNodes);

                    Set<Node> assignedOrdinaryPeerNodes = superPeerRoutingTable.getAllAssignedOrdinaryNetworkNodes();
                    assignedOrdinaryPeerNodes = assignedOrdinaryPeerNodes.stream().parallel()
                            .filter(Node::isActive)
                            .collect(Collectors.toSet());
                    data.put(APIConstants.ASSIGNED_ORDINARY_PEERS, assignedOrdinaryPeerNodes);
                } else {
                    responseStatus = Status.ERROR;
                }
            } else {
                if (routingTable instanceof OrdinaryPeerRoutingTable) {
                    OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
                    Node assignedSuperPeer = ordinaryPeerRoutingTable.getAssignedSuperPeer();
                    if (assignedSuperPeer != null && assignedSuperPeer.isActive()) {
                        data.put(APIConstants.ASSIGNED_SUPER_PEER, assignedSuperPeer);
                    }
                } else {
                    responseStatus = Status.ERROR;
                }
            }
        } else {
            responseStatus = Status.IN_TRACER_MODE;
        }

        Map<String, Object> response = ResponseUtils.generateCustomResponse(responseStatus);
        response.put(APIConstants.DATA, data);
        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
