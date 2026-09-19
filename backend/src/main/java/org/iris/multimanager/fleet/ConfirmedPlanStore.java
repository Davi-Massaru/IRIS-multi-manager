package org.iris.multimanager.fleet;

import java.util.*;

/** Bounded, expiring, single-use plans bound to the originating session. */
public final class ConfirmedPlanStore<T> {
    private record Entry<T>(String session,T value,long expiresAt){}
    private final Map<String,Entry<T>> entries=new HashMap<>();
    public synchronized void put(String id,String session,T value,long expiresAt){
        entries.entrySet().removeIf(e->e.getValue().expiresAt()<System.currentTimeMillis());
        if(entries.size()>=128)throw new TargetFailure("BUSY",429);
        entries.put(id,new Entry<>(session,value,expiresAt));
    }
    public synchronized T consume(String id,String session){
        var entry=entries.get(id);
        if(entry==null||!Objects.equals(entry.session(),session)||entry.expiresAt()<System.currentTimeMillis())throw new TargetFailure("PREVIEW_EXPIRED",409);
        entries.remove(id);return entry.value();
    }
}
