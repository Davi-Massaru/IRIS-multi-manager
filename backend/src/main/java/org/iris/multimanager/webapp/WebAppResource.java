package org.iris.multimanager.webapp;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import org.iris.multimanager.fleet.*;

@Path("/api/web-apps") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class WebAppResource {
    @Inject PreflightService preflight;
    @POST @Path("/preview") public PreflightService.Preview preview(@CookieParam("IRIS_SESSION")String session,PreflightService.Request request){return preflight.preview(session,request);}
    public record Confirm(boolean confirmed){}
    @POST @Path("/execute/{id}") public FleetResult<JsonNode> execute(@CookieParam("IRIS_SESSION")String session,@PathParam("id")String id,Confirm confirm){if(confirm==null||!confirm.confirmed())throw new BadRequestException();return preflight.execute(session,id);}
}
