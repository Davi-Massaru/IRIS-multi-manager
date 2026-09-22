package org.iris.multimanager.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.iris.multimanager.monitor.MonitorModels.*;

class MetricHistoryTest {
    @Test void boundsRecentSamples(){
        var history=new MetricHistory();
        for(int i=0;i<70;i++)history.add("prod1",overview(i));
        assertEquals(60,history.get("prod1").size());
        assertEquals(10,history.get("prod1").getFirst().collectedAt());
        assertTrue(history.get("offline").isEmpty());
    }
    private Overview overview(long time){return new Overview(time,"HEALTHY",null,true,1,0,0,1d,2d,3L,4L,5L,0,0,8,12d,12d,"Normal","Normal","Normal","Normal","Normal");}
}
