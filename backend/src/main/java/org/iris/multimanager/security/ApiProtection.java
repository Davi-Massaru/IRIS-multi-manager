package org.iris.multimanager.security;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiProtection implements ContainerRequestFilter,ContainerResponseFilter {
    @Override public void filter(ContainerRequestContext request) {
        if(!java.util.Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())
            && !"IRIS-Multi-Manager".equals(request.getHeaderString("X-Requested-With")))
            request.abortWith(Response.status(403).entity(java.util.Map.of("error","Explicit same-origin request required")).build());
    }
    @Override public void filter(ContainerRequestContext request,ContainerResponseContext response) {
        response.getHeaders().putSingle("Cache-Control","no-store");
        response.getHeaders().putSingle("X-Content-Type-Options","nosniff");
    }
}
