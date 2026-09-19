package org.iris.multimanager.fleet;

public record FleetTargetResult<T>(String instanceId,String instanceName,String status,int httpStatus,
                                   long elapsedMs,T data,String errorCode,String message) {}
