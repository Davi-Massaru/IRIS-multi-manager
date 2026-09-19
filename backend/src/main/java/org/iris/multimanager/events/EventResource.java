package org.iris.multimanager.events;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.*;

@Path("/api/events") @Produces(MediaType.APPLICATION_JSON)
public class EventResource {
    @Inject InstanceRegistry registry; @Inject FleetExecutor fleet; @Inject SysAdminClientFactory clients;
    @GET public FleetResult<JsonNode> audit(@QueryParam("instances")String ids,@QueryParam("maxRows")@DefaultValue("100")int maxRows,@CookieParam("IRIS_SESSION")String session){
        if(maxRows<1||maxRows>500)throw new BadRequestException("maxRows must be between 1 and 500");
        return fleet.execute(registry.select(ids),i->{var query=new HashMap<String,String>();query.put("maxRows",Integer.toString(maxRows));query.put("ascending","0");return clients.create(i,session).request("POST","/v2/security/audit/records",query,null);});
    }
}
