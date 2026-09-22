package org.iris.multimanager.monitor;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.*;
import static org.iris.multimanager.monitor.MonitorModels.*;

@Path("/api/monitor") @Produces(MediaType.APPLICATION_JSON)
public class MonitorResource {
    @Inject InstanceRegistry registry;
    @Inject FleetExecutor fleet;
    @Inject MonitorService monitor;
    @Inject MetricHistory history;

    @GET @Path("/overview") public FleetResult<Overview> overview(@QueryParam("instances")String ids,@CookieParam("IRIS_SESSION")String session){
        return fleet.execute(registry.select(ids),instance->monitor.overview(instance,session));
    }
    @GET @Path("/licenses") public FleetResult<LicenseDetails> licenses(@QueryParam("instances")String ids,@CookieParam("IRIS_SESSION")String session){
        return fleet.execute(registry.select(ids),instance->monitor.licenses(instance,session));
    }
    @GET @Path("/usage") public FleetResult<UsageCounters> usage(@QueryParam("instances")String ids,@CookieParam("IRIS_SESSION")String session){
        return fleet.execute(registry.select(ids),instance->monitor.usage(instance,session));
    }
    @GET @Path("/history") public FleetResult<List<MetricSample>> history(@QueryParam("instances")String ids){
        var results=registry.select(ids).stream().map(i->new FleetTargetResult<List<MetricSample>>(i.id(),i.name(),"SUCCESS",200,0,history.get(i.id()),null,null)).toList();
        return FleetResult.of(results);
    }
}
