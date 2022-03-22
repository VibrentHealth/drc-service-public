package com.vibrent.drc.domain.common;

import java.io.Serializable;

public abstract class IdGeneratorAbstract implements Serializable {
    private static final long serialVersionUID = 2628229550370857644L;

    protected IdGeneratorAbstract() {
    }

    public Long getId() {
        return null;
    }
}