package org.iris.multimanager;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Map;

@Path("/api/health")
public class HealthResource {
    @GET
    public Map<String, String> health() { return Map.of("status", "UP"); }
}
