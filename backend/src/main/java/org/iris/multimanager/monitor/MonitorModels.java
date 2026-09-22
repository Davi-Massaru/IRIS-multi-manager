package org.iris.multimanager.monitor;

import java.util.List;

public final class MonitorModels {
    private MonitorModels() {}

    public record Overview(
        long collectedAt,
        String health,
        String uptime,
        Boolean systemMonitor,
        Integer processes,
        Integer cspSessions,
        Integer busyProcesses,
        Double globalRefsPerSecond,
        Double cacheEfficiency,
        Long globalRefsSinceStartup,
        Long diskReadsSinceStartup,
        Long diskWritesSinceStartup,
        Integer seriousAlerts,
        Integer applicationErrors,
        Integer licenseLimit,
        Double licenseUsePercent,
        Double licenseHighPercent,
        String databaseSpace,
        String databaseJournal,
        String journalSpace,
        String lockTable,
        String writeDaemon) {}

    public record MetricSample(long collectedAt,Integer processCount,Integer cspSessions,
                               Double globalRefsPerSecond,Double cacheEfficiency,Double licenseUsePercent) {}

    public record LicenseUser(String userId,String type,Integer connections,Integer maxConnections,
                              Integer cspConnections,Integer licenseUnits,Integer activeSeconds,Integer graceSeconds) {}
    public record LicenseProcess(long pid,String process,String userId,String type,Integer connections,
                                 Integer cspConnections,Integer licenseUnits,Integer activeSeconds,Integer graceSeconds) {}
    public record LicenseDetails(List<LicenseUser> usageByUser,List<LicenseProcess> usageByProcess) {}

    public record UsageCounters(long collectedAt,Long allGlobalReferences,Long globalUpdateReferences,
                                Long routineCalls,Long routineBufferLoadsAndSaves,Long routineLines,
                                Long logicalBlockRequests,Long blockReads,Long blockWrites,Long wijWrites,
                                Long journalEntries,Long journalBlockWrites,String lastUpdate,
                                List<SharedMemory> sharedMemory) {}
    public record SharedMemory(String description,Long allocated,Long available,Long used,Long totalUsed) {}
}
