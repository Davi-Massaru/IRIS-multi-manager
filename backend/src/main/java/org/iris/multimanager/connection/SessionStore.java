package org.iris.multimanager.connection;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.iris.multimanager.fleet.TargetFailure;

@ApplicationScoped
public class SessionStore {
    private static final long TTL=30*60*1000L;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final class Session {
        volatile long expires=System.currentTimeMillis()+TTL;
        final Map<String,CredentialContext> credentials=new ConcurrentHashMap<>();
    }
    public synchronized String put(String cookie, String instance, CredentialContext credential) {
        sessions.entrySet().removeIf(e->e.getValue().expires<System.currentTimeMillis());
        if (cookie==null || !sessions.containsKey(cookie)) {
            if (sessions.size()>=256) throw new TargetFailure("BUSY",429);
            cookie=UUID.randomUUID().toString(); sessions.put(cookie,new Session());
        }
        sessions.get(cookie).credentials.put(instance,credential);
        return cookie;
    }
    public CredentialContext require(String cookie, String instance) {
        var session=cookie==null?null:sessions.get(cookie);
        if (session==null || session.expires<System.currentTimeMillis()) {
            if(cookie!=null) sessions.remove(cookie);
            throw new TargetFailure("UNAUTHORIZED",401);
        }
        session.expires=System.currentTimeMillis()+TTL;
        var credential=session.credentials.get(instance);
        if(credential==null) throw new TargetFailure("UNAUTHORIZED",401);
        return credential;
    }
    public void remove(String cookie) { if(cookie!=null) sessions.remove(cookie); }
}
