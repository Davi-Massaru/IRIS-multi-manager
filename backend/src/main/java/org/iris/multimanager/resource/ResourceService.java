package org.iris.multimanager.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.*;

@ApplicationScoped
public class ResourceService {
    @Inject ResourceCatalog catalog;
    @Inject InstanceRegistry registry;
    @Inject FleetExecutor fleet;
    @Inject SysAdminClientFactory clients;
    public ResourceMatrix matrix(String kind,String ids,String session){
        var definition=catalog.get(kind);
        FleetResult<List<JsonNode>> result=fleet.execute(registry.select(ids),i->{
            var listPath=definition.listPath();
            var raw=clients.create(i,session).request("GET",listPath,kind.equals("wallet-collections")?Map.of("maxRows","500"):Map.of(),null);
            if(!raw.isArray())throw new TargetFailure("INVALID_RESPONSE",502);
            var rows=new ArrayList<JsonNode>();for(var row:raw){
                var safe=SafeMetadata.select(row,definition.fields());
                if(kind.equals("tasks"))safe.set("Suspended",clients.create(i,session).request("GET","/v2/task/info",Map.of("id",row.path("Id").asText()),null).path("Suspended"));
                rows.add(safe);
            }return rows;
        });
        return ResourceMatrix.from(result,definition.key());
    }
    public JsonNode detail(IrisInstance instance,String session,String kind,String name)throws Exception{
        var definition=catalog.get(kind);
        if(definition.detailPath()==null)throw new TargetFailure("UNSUPPORTED",405);
        var parameters=kind.equals("tasks")?Map.of("id",task(instance,session,name).path("Id").asText()):Map.of(definition.detailParameter(),name);
        return SafeMetadata.select(clients.create(instance,session).request("GET",definition.detailPath(),parameters,null),definition.detailFields());
    }
    public JsonNode task(IrisInstance instance,String session,String name)throws Exception{
        var tasks=clients.create(instance,session).get("/v2/tasks");
        for(var task:tasks)if(name.equals(task.path("Name").asText())){
            var corrected=(com.fasterxml.jackson.databind.node.ObjectNode)task.deepCopy();
            var info=clients.create(instance,session).request("GET","/v2/task/info",Map.of("id",task.path("Id").asText()),null);
            corrected.set("Suspended",info.path("Suspended"));return corrected;
        }
        throw new TargetFailure("RESOURCE_MISSING",404);
    }
}
