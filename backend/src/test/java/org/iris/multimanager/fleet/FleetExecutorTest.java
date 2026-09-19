package org.iris.multimanager.fleet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.iris.multimanager.connection.*;
import org.iris.multimanager.process.ProcessResource;
import com.fasterxml.jackson.databind.ObjectMapper;

class FleetExecutorTest {
    private IrisInstance instance(String id) { return new IrisInstance(id,id,"http://"+id+":52773/api/admin","TEST",id,1972,"USER",true); }
    private List<IrisInstance> targets() { return List.of(instance("a"),instance("b"),instance("c")); }
    @Test void isolatesOfflineAndPreservesIdentity() {
        var executor=new FleetExecutor(3,1000);
        try { var result=executor.execute(targets(),i->{if(i.id().equals("b")) throw new java.net.ConnectException();return 42;});
            assertEquals(2,result.successCount());assertEquals("OFFLINE",result.results().get(1).status());assertEquals("c",result.results().get(2).instanceId());
        } finally {executor.close();}
    }
    @Test void timesOutWithoutDiscardingCompletedTargets() {
        var executor=new FleetExecutor(3,80);
        try { var result=executor.execute(targets(),i->{if(i.id().equals("a"))Thread.sleep(1000);return i.id();});
            assertEquals("TIMEOUT",result.results().getFirst().status()); assertEquals(2,result.successCount());
        } finally {executor.close();}
    }
    @Test void isolatesAuthErrors() {
        var executor=new FleetExecutor(3,1000);
        try {var result=executor.execute(targets(),i->{if(i.id().equals("a"))throw new TargetFailure("UNAUTHORIZED",401);if(i.id().equals("b"))throw new TargetFailure("FORBIDDEN",403);return true;});
            assertEquals(List.of("UNAUTHORIZED","FORBIDDEN","SUCCESS"),result.results().stream().map(FleetTargetResult::status).toList());
        } finally {executor.close();}
    }
    @Test void concurrencyIsBounded() {
        var executor=new FleetExecutor(2,1000);var active=new AtomicInteger();var max=new AtomicInteger();
        try {var result=executor.execute(targets(),i->{int count=active.incrementAndGet();max.accumulateAndGet(count,Math::max);Thread.sleep(20);active.decrementAndGet();return true;});assertEquals(3,result.successCount());assertEquals(2,max.get());}
        finally {executor.close();}
    }
    @Test void identicalPidsHaveDifferentCompositeIdentity() throws Exception {
        var process=new ObjectMapper().readTree("{\"Pid\":42,\"Routine\":\"DemoWorker\"}");
        var a=ProcessResource.normalize(instance("a"),process);var b=ProcessResource.normalize(instance("b"),process);
        assertEquals(a.get("pid"),b.get("pid"));assertNotEquals(a.get("instanceId"),b.get("instanceId"));
    }
    @Test void rejectsOutboundUrlTricks() {
        for(String url:List.of("file:///api/admin","http://untrusted/api/admin","http://a@evil/api/admin","http://a/api/admin?url=evil","http://a/other"))
            assertThrows(IllegalArgumentException.class,()->InstanceRegistry.validateEndpoint(java.net.URI.create(url),Set.of("a")));
    }
    @Test void credentialsAreSessionAndTargetScoped() {
        var store=new SessionStore();var context=new CredentialContext("test","sensitive");String session=store.put(null,"a",context);
        assertSame(context,store.require(session,"a"));assertThrows(TargetFailure.class,()->store.require(session,"b"));assertThrows(TargetFailure.class,()->store.require("other","a"));
        assertFalse(context.toString().contains("sensitive"));store.remove(session);assertThrows(TargetFailure.class,()->store.require(session,"a"));
    }
}
