package org.iris.multimanager.connection;

public record IrisInstance(String id, String name, String adminBaseUrl, String environment,
                           String jdbcHost, int jdbcPort, String defaultNamespace, boolean enabled) {}
