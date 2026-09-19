package org.iris.multimanager.fleet;

import java.util.List;
public record FleetResult<T>(int requestedTargets,int completedTargets,long successCount,long failureCount,
                              List<FleetTargetResult<T>> results) {
    public static <T> FleetResult<T> of(List<FleetTargetResult<T>> results) {
        long successes=results.stream().filter(r->r.status().equals("SUCCESS")).count();
        return new FleetResult<>(results.size(),results.size(),successes,results.size()-successes,List.copyOf(results));
    }
}
