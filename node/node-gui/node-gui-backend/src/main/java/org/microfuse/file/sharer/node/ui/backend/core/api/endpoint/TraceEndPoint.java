package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.tracing.TraceableState;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerMode;
import org.microfuse.file.sharer.node.ui.backend.core.utils.ResponseUtils;

import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tracing related endpoint.
 */
@Path("/trace")
public class TraceEndPoint {
    @POST
    @Path("/state/{state}")
    public Response changeState(@PathParam("state") String stateString) {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            TraceableState traceable = TraceableState.valueOf(stateString);
            FileSharerHolder.getFileSharer().getServiceHolder().changeTraceableState(traceable);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/state")
    public Response getMode() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            response.put(APIConstants.DATA,
                    FileSharerHolder.getFileSharer().getServiceHolder().getTraceableState());
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_TRACER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/network")
    public Response getNetwork() {
        Map<String, Object> response;

        if (FileSharerHolder.getMode() == FileSharerMode.FILE_SHARER) {
            response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

            Network network = FileSharerHolder.getTracer().getNetwork();
            response.put(APIConstants.DATA, network);
        } else {
            response = ResponseUtils.generateCustomResponse(Status.IN_FILE_SHARER_MODE);
        }

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
