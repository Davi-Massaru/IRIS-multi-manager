package org.iris.multimanager.system;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.*;

@Path("/api/system") @Produces(MediaType.APPLICATION_JSON)
public class SystemResource {
    @Inject InstanceRegistry registry; @Inject FleetExecutor fleet; @Inject SysAdminClientFactory clients;
    private static final Map<String,String> PATHS=Map.of("summary","/v2/monitor/dashboard/main","resources","/v2/monitor/dashboard/system-resources","usage","/v2/monitor/system-usage","devices","/v2/devices");
    @GET @Path("/{kind}") public FleetResult<JsonNode> read(@PathParam("kind")String kind,@QueryParam("instances")String ids,@CookieParam("IRIS_SESSION")String session){
        var path=PATHS.get(kind);if(path==null)throw new BadRequestException("Unsupported system view");
        return fleet.execute(registry.select(ids),i->clients.create(i,session).get(path));
    }
}
