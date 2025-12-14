package org.dreamabout.sw.frp.be.domain.exception;

public class FrpDbException extends RuntimeException {
    public FrpDbException(String s, Exception e) {
        super(s, e);
    }
    public FrpDbException(String s) {
        super(s);
    }
}
