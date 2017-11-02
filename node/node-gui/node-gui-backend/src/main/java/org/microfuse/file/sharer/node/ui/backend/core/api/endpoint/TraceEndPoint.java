package org.microfuse.file.sharer.node.ui.backend.core.api.endpoint;

import com.google.gson.Gson;
import org.microfuse.file.sharer.node.commons.tracing.TracingMode;
import org.microfuse.file.sharer.node.core.tracing.Network;
import org.microfuse.file.sharer.node.ui.backend.commons.APIConstants;
import org.microfuse.file.sharer.node.ui.backend.commons.Status;
import org.microfuse.file.sharer.node.ui.backend.core.utils.FileSharerHolder;
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
    @Path("/mode/{mode}")
    public Response changeMode(@PathParam("mode") String modeString) {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        TracingMode tracingMode = TracingMode.valueOf(modeString);

        if (tracingMode == TracingMode.TRACER) {
            FileSharerHolder.getFileSharer().shutdown();
        } else if (FileSharerHolder.getFileSharer().getServiceHolder().getTraceManager().getMode()
                == TracingMode.TRACER) {
            FileSharerHolder.getFileSharer().start();
        }

        FileSharerHolder.getFileSharer().getServiceHolder().getTraceManager().changeMode(tracingMode);

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/mode")
    public Response getMode() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        response.put(APIConstants.DATA,
                FileSharerHolder.getFileSharer().getServiceHolder().getTraceManager().getMode());

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/network")
    public Response getNetwork() {
        Map<String, Object> response = ResponseUtils.generateCustomResponse(Status.SUCCESS);

        Network network = FileSharerHolder.getFileSharer().getServiceHolder().getTraceManager().getNetwork();
        response.put(APIConstants.DATA, network);

        String jsonString = new Gson().toJson(response);
        return Response.ok(jsonString, MediaType.APPLICATION_JSON).build();
    }
}
