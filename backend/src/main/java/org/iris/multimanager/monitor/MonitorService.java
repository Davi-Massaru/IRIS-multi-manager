package org.iris.multimanager.monitor;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import org.iris.multimanager.connection.IrisInstance;
import org.iris.multimanager.sysadmin.SysAdminClientFactory;
import static org.iris.multimanager.monitor.MonitorModels.*;

@ApplicationScoped
public class MonitorService {
    @Inject SysAdminClientFactory clients;
    @Inject MetricHistory history;

    public Overview overview(IrisInstance instance,String session)throws Exception {
        JsonNode root=clients.create(instance,session).get("/v2/monitor/dashboard/main");
        var result=parseOverview(root,System.currentTimeMillis());
        history.add(instance.id(),result);
        return result;
    }

    static Overview parseOverview(JsonNode root,long collectedAt) {
        JsonNode performance=root.path("Performance"),usage=root.path("SystemUsage"),status=root.path("Status");
        JsonNode alerts=root.path("Alerts"),licensing=root.path("Licensing");
        return new Overview(collectedAt,health(status,usage),text(status,"UpTime"),bool(status,"SystemMonitor"),
            integer(usage,"Processes"),integer(usage,"CSPSessions"),busyProcesses(usage.path("BusyProcesses")),
            decimal(performance,"GlobalRefsPerSecond"),decimal(performance,"CacheEfficiency"),number(performance,"GlobalRefs"),
            number(performance,"DiskReads"),number(performance,"DiskWrites"),integer(alerts,"SeriousAlerts"),integer(alerts,"ApplicationErrors"),
            integer(licensing,"LicenseLimit"),decimal(licensing,"LicenseUse"),decimal(licensing,"LicenseUseHigh"),
            text(usage,"DatabaseSpace"),text(usage,"DatabaseJournal"),text(usage,"JournalSpace"),text(usage,"LockTable"),text(usage,"WriteDaemon"));
    }

    public LicenseDetails licenses(IrisInstance instance,String session)throws Exception {
        JsonNode root=clients.create(instance,session).get("/v2/monitor/license-usage");
        var users=new ArrayList<LicenseUser>();
        for(JsonNode row:root.path("UsageByUser")) users.add(new LicenseUser(text(row,"UserId"),text(row,"Type"),integer(row,"Connects"),
            integer(row,"MaxCon"),integer(row,"CSPCon"),integer(row,"LU"),integer(row,"Active"),integer(row,"Grace")));
        var processes=new ArrayList<LicenseProcess>();
        for(JsonNode row:root.path("UsageByProcess")) if(row.path("LU").asInt()>0||row.path("Con").asInt()>0||row.path("CSPCon").asInt()>0)
            processes.add(new LicenseProcess(longValue(row,"PID"),text(row,"Process"),text(row,"LID"),text(row,"Type"),integer(row,"Con"),
                integer(row,"CSPCon"),integer(row,"LU"),integer(row,"Active"),integer(row,"Grace")));
        return new LicenseDetails(List.copyOf(users),List.copyOf(processes));
    }

    public UsageCounters usage(IrisInstance instance,String session)throws Exception {
        var client=clients.create(instance,session);
        JsonNode root=client.get("/v2/monitor/system-usage"),memory=client.get("/v2/monitor/system-usage/shared-memory");
        var rows=new ArrayList<SharedMemory>();
        for(JsonNode row:memory) if(row.path("SMHAllocated").asLong()>0)
            rows.add(new SharedMemory(text(row,"Description"),number(row,"SMHAllocated"),number(row,"SMHAvailable"),number(row,"SMHUsed"),number(row,"AllUsed")));
        return new UsageCounters(System.currentTimeMillis(),number(root,"AllGlobalReferences"),number(root,"GlobalUpdateReferences"),
            number(root,"RoutineCalls"),number(root,"RoutineBufferLoadsAndSaves"),number(root,"RoutineLines"),number(root,"LogicalBlockRequests"),
            number(root,"BlockReads"),number(root,"BlockWrites"),number(root,"WIJwrites"),number(root,"JournalEntries"),number(root,"JournalBlockWrites"),
            text(root,"LastUpdate"),List.copyOf(rows));
    }

    private static String health(JsonNode status,JsonNode usage){
        if(Boolean.FALSE.equals(bool(status,"SystemMonitor")))return "WARNING";
        for(String key:List.of("DatabaseSpace","DatabaseJournal","JournalSpace","LockTable","WriteDaemon")){
            String value=text(usage,key);if(value!=null&&!value.equalsIgnoreCase("Normal"))return "WARNING";
        }
        return "HEALTHY";
    }
    private static Integer busyProcesses(JsonNode rows){if(!rows.isArray())return null;int count=0;for(JsonNode row:rows)if(row.path("Commands").asLong()>0)count++;return count;}
    static String text(JsonNode node,String key){var value=node.get(key);return value==null||value.isNull()||value.asText().isBlank()?null:value.asText();}
    static Integer integer(JsonNode node,String key){var value=node.get(key);return value==null||!value.isNumber()?null:value.intValue();}
    static Long number(JsonNode node,String key){var value=node.get(key);return value==null||!value.isNumber()?null:value.longValue();}
    static long longValue(JsonNode node,String key){Long value=number(node,key);return value==null?0:value;}
    static Double decimal(JsonNode node,String key){var value=node.get(key);return value==null||!value.isNumber()?null:value.doubleValue();}
    static Boolean bool(JsonNode node,String key){var value=node.get(key);return value==null||!value.isBoolean()?null:value.booleanValue();}
}
