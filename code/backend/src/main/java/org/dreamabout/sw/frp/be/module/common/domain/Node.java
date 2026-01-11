package org.dreamabout.sw.frp.be.module.common.domain;

public interface Node {

    Node getParent();

    Boolean isPlaceholder();

    default  Boolean isRoot() {
        return getParent() == null;
    }
}
