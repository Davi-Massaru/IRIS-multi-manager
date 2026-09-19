package org.iris.multimanager.fleet;

public final class TargetFailure extends RuntimeException {
    public final String code;
    public final int httpStatus;
    public TargetFailure(String code,int httpStatus) { super(code); this.code=code; this.httpStatus=httpStatus; }
}
