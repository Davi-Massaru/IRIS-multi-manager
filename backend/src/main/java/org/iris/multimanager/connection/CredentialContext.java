package org.iris.multimanager.connection;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Deliberately not a record: generated toString must never reveal credentials. */
public final class CredentialContext {
    private final String username;
    private final String password;
    public CredentialContext(String username, String password) { this.username=username; this.password=password; }
    public String username() { return username; }
    public String password() { return password; }
    public String authorization() { return "Basic " + Base64.getEncoder().encodeToString((username+":"+password).getBytes(StandardCharsets.UTF_8)); }
    @Override public String toString() { return "CredentialContext[REDACTED]"; }
}
