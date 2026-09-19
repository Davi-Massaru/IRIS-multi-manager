package org.iris.multimanager.task;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.resource.*;
import org.iris.multimanager.sysadmin.*;

@jakarta.enterprise.context.ApplicationScoped
@Path("/api/tasks") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {
    public record Request(List<String> targets,String name,String action){}
    private record Plan(List<IrisInstance> targets,String name,String action,PreflightService.Preview preview){}
    private final ConfirmedPlanStore<Plan> plans=new ConfirmedPlanStore<>();
    @Inject InstanceRegistry registry;@Inject SessionStore sessions;@Inject ResourceService resources;
    @Inject FleetExecutor fleet;@Inject SysAdminClientFactory clients;@Inject ObjectMapper mapper;
    @POST @Path("/preview") public PreflightService.Preview preview(@CookieParam("IRIS_SESSION")String session,Request request){
        if(request==null||request.targets()==null||request.targets().isEmpty()||request.targets().size()>32||request.name()==null||request.name().length()>256||!Set.of("run","suspend","resume").contains(request.action()))throw new BadRequestException();
        var targets=request.targets().stream().distinct().map(registry::get).toList();targets.forEach(i->sessions.require(session,i.id()));
        var result=fleet.execute(targets,i->{
            var task=resources.task(i,session,request.name());
            ObjectNode before=SafeMetadata.select(task,Set.of("Id","Name","Suspended","LastFinished","NextScheduled"));
            before.set("configuration",resources.detail(i,session,"tasks",request.name()));
            var after=before.deepCopy();after.put("requestedAction",request.action());
            return new PreflightService.PreviewTarget(before,after,"READY");
        });
        String id=UUID.randomUUID().toString();var preview=new PreflightService.Preview(id,request.name(),mapper.createObjectNode().put("action",request.action()),result,System.currentTimeMillis()+120000);
        plans.put(id,session,new Plan(targets,request.name(),request.action(),preview),preview.expiresAt());return preview;
    }
    public record Confirm(boolean confirmed){}
    @POST @Path("/execute/{id}") public FleetResult<JsonNode> execute(@CookieParam("IRIS_SESSION")String session,@PathParam("id")String id,Confirm confirmation){
        if(confirmation==null||!confirmation.confirmed())throw new BadRequestException();var plan=plans.consume(id,session);
        return fleet.execute(plan.targets(),i->{
            var previous=plan.preview().targets().results().stream().filter(r->r.instanceId().equals(i.id())).findFirst().orElseThrow();
            if(!previous.status().equals("SUCCESS"))throw new TargetFailure("SKIPPED",409);
            var current=resources.task(i,session,plan.name());
            if(!current.path("Id").equals(previous.data().before().path("Id"))||!resources.detail(i,session,"tasks",plan.name()).equals(previous.data().before().path("configuration")))throw new TargetFailure("STALE_CONFIGURATION",409);
            JsonNode body=plan.action().equals("run")?mapper.createObjectNode().put("RunNow",true):plan.action().equals("suspend")?mapper.createObjectNode().put("LeaveInQueue",true):null;
            clients.create(i,session).request("POST","/v2/task/"+plan.action(),Map.of("id",current.path("Id").asText()),body);
            var after=resources.task(i,session,plan.name());
            if(!plan.action().equals("run")&&after.path("Suspended").asBoolean()!=plan.action().equals("suspend"))throw new TargetFailure("VERIFICATION_FAILED",409);
            var result=SafeMetadata.select(after,Set.of("Name","Suspended","LastFinished","NextScheduled"));
            result.put("instanceId",i.id()).put("instanceName",i.name()).put("action",plan.action()).put("accepted",true);
            if(plan.action().equals("run"))result.put("executionStatus","Accepted; task completion is asynchronous");
            return result;
        });
    }
}
