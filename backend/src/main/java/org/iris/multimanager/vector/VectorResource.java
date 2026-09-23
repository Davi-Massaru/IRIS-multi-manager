package org.iris.multimanager.vector;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;
import static org.iris.multimanager.vector.VectorModels.*;

@Path("/api/vector") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class VectorResource {
    @Inject VectorService service;
    @POST @Path("/libraries/check") public Object checkLibrary(@CookieParam("IRIS_SESSION") String session,Map<String,String> body){if(body==null)throw new BadRequestException();return service.checkLibrary(session,body.get("instances"),body.get("module"));}
    @GET @Path("/assets") public Object assets(@CookieParam("IRIS_SESSION") String session,@QueryParam("instances") String instances){return service.inventory(session,instances);}
    @GET @Path("/assets/{instance}/{asset}/rows") public Object rows(@CookieParam("IRIS_SESSION") String session,@PathParam("instance") String instance,@PathParam("asset") String asset,@QueryParam("includeVector") @DefaultValue("false") boolean includeVector){return service.rows(session,instance,asset,includeVector);}
    @GET @Path("/extension/status") public Object status(@CookieParam("IRIS_SESSION") String session,@QueryParam("instances") String instances){return service.extension(session,instances);}
    @POST @Path("/models/test") public Object test(@CookieParam("IRIS_SESSION") String session,Map<String,String> body){return service.testModel(session,body.get("instanceId"),body.get("text"));}
    @POST @Path("/models/check") public Object check(@CookieParam("IRIS_SESSION") String session,Map<String,String> body){return service.checkModel(session,body.get("instanceId"),body.get("model"));}
    @POST @Path("/models/download") public Object download(@CookieParam("IRIS_SESSION") String session,Map<String,String> body){return service.downloadModel(session,body.get("instanceId"),body.get("model"));}
    @POST @Path("/indexes/preview") public Object previewIndex(@CookieParam("IRIS_SESSION") String session,IndexRequest request) throws Exception{return service.previewIndex(session,request);}
    @POST @Path("/regeneration/preview") public Object previewRegenerate(@CookieParam("IRIS_SESSION") String session,RegenerateRequest request) throws Exception{return service.previewRegenerate(session,request);}
    @POST @Path("/actions/apply") public Object apply(@CookieParam("IRIS_SESSION") String session,ApplyRequest request){return service.apply(session,request);}
}
