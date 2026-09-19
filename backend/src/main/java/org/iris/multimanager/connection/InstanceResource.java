package org.iris.multimanager.connection;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.iris.multimanager.fleet.*;
import org.iris.multimanager.sysadmin.*;

@Path("/api/instances") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class InstanceResource {
    @Inject InstanceRegistry registry;
    @Inject SessionStore sessions;
    @Inject SysAdminClientFactory clients;
    @Inject FleetExecutor fleet;
    public record Login(@NotBlank @Size(max=128) String username,@NotBlank @Size(max=1024) String password) {}
    @GET public List<Map<String,Object>> list() { return registry.all().stream().map(i->Map.<String,Object>of("id",i.id(),"name",i.name(),"environment",i.environment(),"defaultNamespace",i.defaultNamespace(),"enabled",i.enabled())).toList(); }
    @POST @Path("/{id}/connect")
    public Response connect(@PathParam("id") String id,@CookieParam("IRIS_SESSION") String cookie,@Valid Login login,@Context UriInfo uri) throws Exception {
        if(login==null) throw new BadRequestException();
        var instance=registry.get(id); var credential=new CredentialContext(login.username(),login.password());
        JsonNode info=clients.withCredentials(instance,credential).get("/info");
        String session=sessions.put(cookie,id,credential);
        return Response.ok(Map.of("instanceId",id,"instanceName",instance.name(),"data",info))
            .cookie(new NewCookie.Builder("IRIS_SESSION").value(session).path("/api").httpOnly(true).sameSite(NewCookie.SameSite.STRICT).secure("https".equals(uri.getBaseUri().getScheme())).maxAge(1800).build()).build();
    }
    @POST @Path("/{id}/validate") public FleetResult<JsonNode> validate(@PathParam("id") String id,@CookieParam("IRIS_SESSION") String cookie) {
        return fleet.execute(List.of(registry.get(id)),i->clients.create(i,cookie).get("/info"));
    }
    @GET @Path("/status") public FleetResult<JsonNode> status(@CookieParam("IRIS_SESSION") String cookie) {
        return fleet.execute(registry.all(),i->clients.create(i,cookie).get("/info"));
    }
    @POST @Path("/disconnect") public Response disconnect(@CookieParam("IRIS_SESSION") String cookie) {
        sessions.remove(cookie); return Response.noContent().cookie(new NewCookie.Builder("IRIS_SESSION").value("").path("/api").maxAge(0).httpOnly(true).build()).build();
    }
}
