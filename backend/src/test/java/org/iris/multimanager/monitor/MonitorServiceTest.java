package org.iris.multimanager.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonitorServiceTest {
    private final ObjectMapper mapper=new ObjectMapper();

    @Test void normalizesDocumentedMetricsAndKeepsCounterSemantics()throws Exception{
        var json=mapper.readTree("""
          {"Performance":{"GlobalRefsPerSecond":18.2,"GlobalRefs":123182,"DiskReads":40,"DiskWrites":9,"CacheEfficiency":91.5},
           "Status":{"UpTime":"1d","SystemMonitor":true},
           "SystemUsage":{"Processes":42,"CSPSessions":3,"BusyProcesses":[{"Process":7,"Commands":12}],"DatabaseSpace":"Normal","DatabaseJournal":"Normal","JournalSpace":"Normal","LockTable":"Normal","WriteDaemon":"Normal"},
           "Alerts":{"SeriousAlerts":0,"ApplicationErrors":2},"Licensing":{"LicenseLimit":100,"LicenseUse":61,"LicenseUseHigh":79}}
          """);
        var value=MonitorService.parseOverview(json,1000);
        assertEquals("HEALTHY",value.health());
        assertEquals(18.2,value.globalRefsPerSecond());
        assertEquals(123182,value.globalRefsSinceStartup());
        assertEquals(61,value.licenseUsePercent());
        assertEquals(1,value.busyProcesses());
    }

    @Test void emptyLicenseUseIsUnavailableAndMissingMetricsRemainNull()throws Exception{
        var json=mapper.readTree("""
          {"Performance":{},"Status":{"SystemMonitor":false},"SystemUsage":{},"Alerts":{},"Licensing":{"LicenseUse":"","LicenseUseHigh":""}}
          """);
        var value=MonitorService.parseOverview(json,1000);
        assertNull(value.licenseUsePercent());
        assertNull(value.licenseHighPercent());
        assertNull(value.processes());
        assertEquals("WARNING",value.health());
    }
}
