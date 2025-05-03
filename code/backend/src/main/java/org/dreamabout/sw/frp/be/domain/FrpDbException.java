package org.dreamabout.sw.frp.be.domain;

public class FrpDbException extends RuntimeException {
    public FrpDbException(String s, Exception e) {
        super(s, e);
    }
}
