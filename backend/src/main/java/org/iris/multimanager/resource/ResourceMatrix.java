package org.iris.multimanager.resource;

import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.iris.multimanager.fleet.*;

public record ResourceMatrix(FleetResult<List<JsonNode>> fleet,List<Row> rows) {
    public record Cell(String instanceId,String instanceName,String presence,JsonNode configuration) {}
    public record Row(String resourceKey,List<Cell> targets) {}
    public static ResourceMatrix from(FleetResult<List<JsonNode>> fleet,String key){
        var names=new TreeSet<String>();
        for(var target:fleet.results()) if(target.data()!=null)for(var resource:target.data())names.add(resource.path(key).asText());
        var rows=new ArrayList<Row>();
        for(String name:names){
            var cells=new ArrayList<Cell>();JsonNode reference=null;
            for(var target:fleet.results()){
                JsonNode config=null;String state=target.status();
                if(state.equals("SUCCESS")){
                    config=target.data().stream().filter(r->r.path(key).asText().equals(name)).findFirst().orElse(null);
                    state=config==null?"MISSING":"PRESENT";
                    if(config!=null){if(reference==null)reference=config;else if(!reference.equals(config))state="DIFFERENT";}
                }
                cells.add(new Cell(target.instanceId(),target.instanceName(),state,config));
            }
            rows.add(new Row(name,List.copyOf(cells)));
        }
        return new ResourceMatrix(fleet,List.copyOf(rows));
    }
}
