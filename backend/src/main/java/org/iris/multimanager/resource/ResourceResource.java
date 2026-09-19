package org.iris.multimanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;

@Path("/api/resources") @Produces(MediaType.APPLICATION_JSON)
public class ResourceResource {
    @Inject ResourceService resources;
    @Inject InstanceRegistry registry;
    @Inject FleetExecutor fleet;
    @GET @Path("/{kind}") public ResourceMatrix matrix(@PathParam("kind")String kind,@QueryParam("instances")String ids,@CookieParam("IRIS_SESSION")String cookie){return resources.matrix(kind,ids,cookie);}
    @GET @Path("/{kind}/detail") public FleetResult<JsonNode> detail(@PathParam("kind")String kind,@QueryParam("instances")String ids,@QueryParam("name")String name,@CookieParam("IRIS_SESSION")String cookie){
        if(name==null||name.isBlank()||name.length()>256)throw new BadRequestException();
        return fleet.execute(registry.select(ids),i->resources.detail(i,cookie,kind,name));
    }
}
