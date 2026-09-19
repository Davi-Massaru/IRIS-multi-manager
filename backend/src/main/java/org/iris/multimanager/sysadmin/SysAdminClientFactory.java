package org.iris.multimanager.sysadmin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.http.HttpClient;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iris.multimanager.connection.*;

@ApplicationScoped
public class SysAdminClientFactory {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    @Inject ObjectMapper mapper;
    @Inject SessionStore sessions;
    public SysAdminClient create(IrisInstance instance,String session) { return withCredentials(instance,sessions.require(session,instance.id())); }
    public SysAdminClient withCredentials(IrisInstance instance,CredentialContext credentials) { return new SysAdminClient(http,mapper,instance,credentials); }
}
