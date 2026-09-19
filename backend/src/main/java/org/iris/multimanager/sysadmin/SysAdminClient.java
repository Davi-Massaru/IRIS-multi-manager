package org.iris.multimanager.sysadmin;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.fleet.TargetFailure;

/** One instance, one authentication context. No fleet orchestration. */
public final class SysAdminClient {
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final IrisInstance instance;
    private final CredentialContext credentials;
    SysAdminClient(HttpClient http,ObjectMapper mapper,IrisInstance instance,CredentialContext credentials) {
        this.http=http;this.mapper=mapper;this.instance=instance;this.credentials=credentials;
    }
    public JsonNode get(String path) throws Exception { return request("GET",path,Map.of(),null); }
    public JsonNode request(String method,String path,Map<String,String> query,JsonNode body) throws Exception {
        if(!path.matches("/(info|login|refresh|logout|v2/[a-z0-9/-]+)")) throw new IllegalArgumentException("Invalid admin path");
        var url=new StringBuilder(instance.adminBaseUrl()).append(path);
        query.forEach((key,value)->url.append(url.indexOf("?")<0?'?':'&').append(encode(key)).append('=').append(encode(value)));
        var request=HttpRequest.newBuilder(URI.create(url.toString())).timeout(Duration.ofSeconds(10))
            .header("Authorization",credentials.authorization()).header("Accept","application/json")
            .header("Content-Type","application/json")
            .method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(var stream=response.body()) {
            int status=response.statusCode();
            if(status<200 || status>=300) throw new TargetFailure(switch(status){case 401->"UNAUTHORIZED";case 403->"FORBIDDEN";case 404->"NOT_FOUND";case 405,501->"UNSUPPORTED";default->"FAILED";},status);
            byte[] bytes=stream.readNBytes(4*1024*1024+1);
            if(bytes.length>4*1024*1024) throw new TargetFailure("RESPONSE_LIMIT",502);
            if(bytes.length==0) return mapper.createObjectNode();
            JsonNode envelope=mapper.readTree(bytes);
            if(envelope.path("status").path("errors").size()>0 || envelope.path("status").path("Errors").size()>0) throw new TargetFailure("FAILED",502);
            return envelope.has("result")?envelope.get("result"):envelope;
        }
    }
    private static String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
}
