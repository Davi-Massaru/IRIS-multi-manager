package org.iris.multimanager.process;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.*;

@Path("/api/processes") @Produces(MediaType.APPLICATION_JSON)
public class ProcessResource {
    @Inject InstanceRegistry registry;
    @Inject SysAdminClientFactory clients;
    @Inject FleetExecutor fleet;
    @Inject ObjectMapper mapper;
    @GET public FleetResult<JsonNode> list(@QueryParam("instances") String ids,@QueryParam("filter") @DefaultValue("") String filter,@CookieParam("IRIS_SESSION") String cookie) {
        if(filter.length()>200) throw new BadRequestException();
        return fleet.execute(registry.select(ids),i->{
            JsonNode raw=clients.create(i,cookie).get("/v2/processes");
            if(!raw.isArray()) throw new TargetFailure("INVALID_RESPONSE",502);
            ArrayNode rows=mapper.createArrayNode();
            for(JsonNode p:raw) if(p.toString().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) rows.add(normalize(i,p));
            return rows;
        });
    }
    public static ObjectNode normalize(IrisInstance i,JsonNode p) {
        var row=JsonNodeFactory.instance.objectNode();
        row.put("instanceId",i.id()).put("instanceName",i.name());
        row.set("pid",p.path("Pid")); row.set("namespace",p.path("Nspace")); row.set("routine",p.path("Routine"));
        row.set("user",p.path("Username")); row.set("state",p.path("State")); row.set("cpuTime",p.path("CPUTime"));row.set("clientIPAddress",p.path("IPAddress"));
        row.set("canSuspend",p.path("CanBeSuspended"));row.set("canTerminate",p.path("CanBeTerminated"));
        return row;
    }
    @GET @Path("/{instanceId}/{pid}") public FleetResult<JsonNode> detail(@PathParam("instanceId") String id,@PathParam("pid") long pid,@CookieParam("IRIS_SESSION") String cookie) {
        if(pid<1) throw new BadRequestException();
        return fleet.execute(List.of(registry.get(id)),i->clients.create(i,cookie).request("GET","/v2/process",Map.of("id",Long.toString(pid)),null));
    }
    public record Confirmation(String confirmation,String expectedStartTimeUTC) {}
    @POST @Path("/{instanceId}/{pid}/{action}") @Consumes(MediaType.APPLICATION_JSON)
    public FleetResult<JsonNode> action(@PathParam("instanceId") String id,@PathParam("pid") long pid,
                                       @PathParam("action") String action,@CookieParam("IRIS_SESSION") String cookie,Confirmation confirmation) {
        var instance=registry.get(id);
        if(pid<1 || !Set.of("suspend","resume","terminate").contains(action) || confirmation==null
            || !(instance.name()+" / PID "+pid).equals(confirmation.confirmation())
            || confirmation.expectedStartTimeUTC()==null || confirmation.expectedStartTimeUTC().isBlank()) throw new BadRequestException();
        return fleet.execute(List.of(instance),i->{
            var client=clients.create(i,cookie);var parameters=Map.of("id",Long.toString(pid));
            var before=client.request("GET","/v2/process",parameters,null);
            if(!confirmation.expectedStartTimeUTC().equals(before.path("StartTimeUTC").asText())) throw new TargetFailure("STALE_PROCESS",409);
            client.request("POST","/v2/process/"+action,parameters,null);
            ObjectNode result=mapper.createObjectNode().put("instanceId",i.id()).put("instanceName",i.name()).put("pid",pid).put("action",action).put("accepted",true);
            if(!action.equals("terminate")) result.set("after",client.request("GET","/v2/process",parameters,null));
            return result;
        });
    }
}
