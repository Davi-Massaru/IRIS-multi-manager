package org.iris.multimanager.fleet;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.iris.multimanager.connection.IrisInstance;

@ApplicationScoped
public class FleetExecutor {
    @FunctionalInterface public interface Operation<T> { T run(IrisInstance instance) throws Exception; }
    private final ThreadPoolExecutor pool;
    private final long timeoutMs;
    @Inject public FleetExecutor(@ConfigProperty(name="fleet.concurrency",defaultValue="6") int concurrency,
                                @ConfigProperty(name="fleet.timeout-ms",defaultValue="15000") long timeoutMs) {
        if(concurrency<1 || concurrency>32 || timeoutMs<1 || timeoutMs>60000) throw new IllegalArgumentException("Invalid fleet bounds");
        this.timeoutMs=timeoutMs;
        pool=new ThreadPoolExecutor(concurrency,concurrency,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),Thread.ofPlatform().daemon().factory(),new ThreadPoolExecutor.AbortPolicy());
    }
    public <T> FleetResult<T> execute(List<IrisInstance> targets,Operation<T> operation) {
        long started=System.nanoTime();
        var futures=new ArrayList<Future<FleetTargetResult<T>>>();
        for(var target:targets) {
            try { futures.add(pool.submit(()->run(target,operation))); }
            catch(RejectedExecutionException e) { futures.add(CompletableFuture.completedFuture(failure(target,"BUSY",429,0))); }
        }
        var results=new ArrayList<FleetTargetResult<T>>();
        for(int n=0;n<targets.size();n++) {
            var future=futures.get(n); var target=targets.get(n);
            try {
                long remaining=Math.max(0,timeoutMs-TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
                results.add(future.get(remaining,TimeUnit.MILLISECONDS));
            } catch(TimeoutException e) {
                future.cancel(true); results.add(failure(target,"TIMEOUT",504,timeoutMs));
            } catch(InterruptedException e) {
                futures.forEach(f->f.cancel(true)); Thread.currentThread().interrupt();
                results.add(failure(target,"FAILED",503,timeoutMs));
            } catch(ExecutionException e) { results.add(failure(target,"FAILED",502,timeoutMs)); }
        }
        return FleetResult.of(results);
    }
    private <T> FleetTargetResult<T> run(IrisInstance target,Operation<T> operation) {
        long start=System.nanoTime();
        try { T data=operation.run(target); return new FleetTargetResult<>(target.id(),target.name(),"SUCCESS",200,elapsed(start),data,null,null); }
        catch(TargetFailure e) { return failure(target,e.code,e.httpStatus,elapsed(start)); }
        catch(java.net.http.HttpTimeoutException e) { return failure(target,"TIMEOUT",504,elapsed(start)); }
        catch(java.io.IOException e) { return failure(target,"OFFLINE",503,elapsed(start)); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); return failure(target,"TIMEOUT",504,elapsed(start)); }
        catch(Exception e) { return failure(target,"FAILED",502,elapsed(start)); }
    }
    private static long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start); }
    private static <T> FleetTargetResult<T> failure(IrisInstance target,String code,int http,long elapsed) {
        return new FleetTargetResult<>(target.id(),target.name(),code,http,elapsed,null,code,"Target operation: "+code);
    }
    @PreDestroy public void close() { pool.shutdownNow(); }
}
