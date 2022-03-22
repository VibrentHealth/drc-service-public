package com.vibrent.drc.domain.common;


import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@MappedSuperclass
@EntityListeners({AuditingEntityListener.class})
public abstract class AbstractEntity extends IdGeneratorAbstract {
    @Column(
            name = "created_on"
    )
    protected Long createdOn;
    @Column(
            name = "updated_on"
    )
    protected Long updatedOn;

    protected AbstractEntity() {
    }

    @PrePersist
    public void prePersist() {
        long currentTimestamp = Instant.now().toEpochMilli();
        this.createdOn = this.createdOn == null ? currentTimestamp : this.createdOn;
        this.updatedOn = currentTimestamp;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = Instant.now().toEpochMilli();
    }

    public Long getCreatedOn() {
        return this.createdOn;
    }

    public Long getUpdatedOn() {
        return this.updatedOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public void setUpdatedOn(Long updatedOn) {
        this.updatedOn = updatedOn;
    }
}