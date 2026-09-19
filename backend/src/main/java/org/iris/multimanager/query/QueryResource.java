package org.iris.multimanager.query;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/query") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class QueryResource {
    @Inject FleetQueryService service;
    @POST public FleetQueryService.QueryResult query(@CookieParam("IRIS_SESSION")String session,FleetQueryService.QueryRequest request){return service.execute(session,request);}
}
