package org.iris.multimanager.security;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import org.iris.multimanager.fleet.TargetFailure;
import java.util.Map;

@Provider
public class ApiErrors implements ExceptionMapper<Exception> {
    public Response toResponse(Exception exception) {
        int status=exception instanceof TargetFailure t?t.httpStatus:exception instanceof WebApplicationException w?w.getResponse().getStatus():500;
        return Response.status(status).entity(Map.of("error",exception instanceof TargetFailure t?t.code:status<500?"Invalid request":"Operation failed")).build();
    }
}
