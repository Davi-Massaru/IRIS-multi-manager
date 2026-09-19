package org.iris.multimanager.connection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.net.URI;
import java.util.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InstanceRegistry {
    private final List<IrisInstance> instances;
    @Inject
    public InstanceRegistry(ObjectMapper mapper, @ConfigProperty(name="fleet.instances") String config,
                            @ConfigProperty(name="fleet.allowed-hosts") String hosts) {
        try {
            instances = List.copyOf(mapper.readValue(config, new TypeReference<List<IrisInstance>>() {}));
            var allowed = Set.of(hosts.split(","));
            var ids = new HashSet<String>();
            if (instances.isEmpty() || instances.size() > 32) throw new IllegalArgumentException();
            for (var instance : instances) {
                if (!ids.add(instance.id()) || !instance.id().matches("[a-z0-9-]+")) throw new IllegalArgumentException();
                validateEndpoint(URI.create(instance.adminBaseUrl()), allowed);
                if (!allowed.contains(instance.jdbcHost()) || instance.jdbcPort()<1 || instance.jdbcPort()>65535
                    || !instance.defaultNamespace().matches("[%A-Za-z0-9_-]+")) throw new IllegalArgumentException();
            }
        } catch (Exception e) { throw new IllegalArgumentException("Invalid fleet registry configuration"); }
    }
    public static void validateEndpoint(URI uri, Set<String> hosts) {
        if (!Set.of("http","https").contains(uri.getScheme()) || !hosts.contains(uri.getHost())
            || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null
            || !"/api/admin".equals(uri.getPath())) throw new IllegalArgumentException("Endpoint is not allowlisted");
    }
    public List<IrisInstance> all() { return instances.stream().filter(IrisInstance::enabled).toList(); }
    public IrisInstance get(String id) { return all().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new BadRequestException("Unknown instance")); }
    public List<IrisInstance> select(String ids) {
        if (ids==null || ids.isBlank()) return all();
        return Arrays.stream(ids.split(",")).distinct().map(this::get).toList();
    }
}
