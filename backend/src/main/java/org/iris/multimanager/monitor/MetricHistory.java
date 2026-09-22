package org.iris.multimanager.monitor;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.iris.multimanager.monitor.MonitorModels.*;

@ApplicationScoped
public class MetricHistory {
    private static final int MAX_SAMPLES=60;
    private final Map<String,Deque<MetricSample>> samples=new ConcurrentHashMap<>();

    public void add(String instanceId,Overview value) {
        var queue=samples.computeIfAbsent(instanceId,key->new ArrayDeque<>());
        synchronized(queue) {
            queue.addLast(new MetricSample(value.collectedAt(),value.processes(),value.cspSessions(),
                value.globalRefsPerSecond(),value.cacheEfficiency(),value.licenseUsePercent()));
            while(queue.size()>MAX_SAMPLES) queue.removeFirst();
        }
    }
    public List<MetricSample> get(String instanceId) {
        var queue=samples.get(instanceId);
        if(queue==null)return List.of();
        synchronized(queue){return List.copyOf(queue);}
    }
}
