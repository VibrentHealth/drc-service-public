package com.vibrent.drc.domain;


import com.vibrent.drc.domain.common.IdGeneratorAbstract;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.util.DateTimeUtil;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "participant_genomic_status_payload")
@Data
public class ParticipantGenomicStatusPayload extends IdGeneratorAbstract {

    private static final long serialVersionUID = 2691818734341436777L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "IdOrGenerated")
    @GenericGenerator(name = "IdOrGenerated", strategy = "com.vibrent.drc.domain.UseIdOrGenerate")
    private Long id;


    @Column(name = "requested_timestamp", nullable = false)
    private String requestedTimestamp;

    @Column(name = "next_timestamp", nullable = false)
    private String nextTimestamp;

    @Column(name = "raw_payload", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String rawPayload;

    @Column(name = "status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ExternalGenomicPayloadProcessingStatus status;

    @Column(name = "created_on")
    protected Long createdOn;

    @Column(name = "updated_on")
    protected Long updatedOn;

    @PrePersist
    public void setTime() {
        this.createdOn = DateTimeUtil.getCurrentTimestamp();
        this.updatedOn = DateTimeUtil.getCurrentTimestamp();
    }

    @PreUpdate
    public void updateTime() {
        this.updatedOn = DateTimeUtil.getCurrentTimestamp();
    }
}

