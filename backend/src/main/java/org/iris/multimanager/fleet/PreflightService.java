package org.iris.multimanager.fleet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.resource.*;
import org.iris.multimanager.sysadmin.*;

@ApplicationScoped
public class PreflightService {
    public record Request(List<String> targets,String name,JsonNode changes) {}
    public record PreviewTarget(JsonNode before,JsonNode after,String readiness) {}
    public record Preview(String operationId,String resourceName,JsonNode changes,FleetResult<PreviewTarget> targets,long expiresAt) {}
    private record Plan(String session,Request request,List<IrisInstance> instances,Preview preview) {}
    private final ConfirmedPlanStore<Plan> plans=new ConfirmedPlanStore<>();
    @Inject InstanceRegistry registry;
    @Inject ResourceService resources;
    @Inject SysAdminClientFactory clients;
    @Inject FleetExecutor fleet;
    @Inject SessionStore sessions;
    public Preview preview(String session,Request request){
        validate(request);
        var targets=request.targets().stream().distinct().map(registry::get).toList();
        targets.forEach(i->sessions.require(session,i.id()));
        Request snapshot=new Request(List.copyOf(request.targets()),request.name(),request.changes().deepCopy());
        var result=fleet.execute(targets,i->{
            JsonNode before;
            try {before=resources.detail(i,session,"web-apps",snapshot.name());}
            catch(TargetFailure e){if(e.httpStatus==404)throw new TargetFailure("RESOURCE_MISSING",404);throw e;}
            var after=before.deepCopy();snapshot.changes().fields().forEachRemaining(e->((com.fasterxml.jackson.databind.node.ObjectNode)after).set(e.getKey(),e.getValue()));
            return new PreviewTarget(before,after,"READY");
        });
        String id=UUID.randomUUID().toString();var preview=new Preview(id,snapshot.name(),snapshot.changes(),result,System.currentTimeMillis()+120000);
        plans.put(id,session,new Plan(session,snapshot,targets,preview),preview.expiresAt());return preview;
    }
    public FleetResult<JsonNode> execute(String session,String id){
        Plan plan=plans.consume(id,session);
        return fleet.execute(plan.instances(),i->{
            var previous=plan.preview().targets().results().stream().filter(r->r.instanceId().equals(i.id())).findFirst().orElseThrow();
            if(!previous.status().equals("SUCCESS"))throw new TargetFailure("SKIPPED",409);
            var before=resources.detail(i,session,"web-apps",plan.request().name());
            if(!before.equals(previous.data().before()))throw new TargetFailure("STALE_CONFIGURATION",409);
            clients.create(i,session).request("PUT","/v2/web-app",Map.of("name",plan.request().name()),plan.request().changes());
            var after=resources.detail(i,session,"web-apps",plan.request().name());
            var changes=plan.request().changes().fields();while(changes.hasNext()){var change=changes.next();if(!change.getValue().equals(after.path(change.getKey())))throw new TargetFailure("VERIFICATION_FAILED",409);}
            return after;
        });
    }
    static void validate(Request request){
        if(request==null||request.targets()==null||request.targets().isEmpty()||request.targets().size()>32||request.name()==null||!request.name().startsWith("/")||request.name().length()>256
            ||request.changes()==null||!request.changes().isObject()||request.changes().isEmpty())throw new BadRequestException();
        request.changes().fields().forEachRemaining(e->{
            boolean valid=switch(e.getKey()){
                case "Enabled"->e.getValue().isBoolean();
                case "Description"->e.getValue().isTextual()&&e.getValue().asText().length()<=256;
                case "Timeout"->e.getValue().isIntegralNumber()&&e.getValue().asLong()>=60&&e.getValue().asLong()<=86400;
                default->false;
            };if(!valid)throw new BadRequestException("Unsupported change");
        });
    }
}
