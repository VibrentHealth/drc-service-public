package com.vibrent.drc.domain;



import com.vibrent.drc.domain.common.IdGeneratorAbstract;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.util.DateTimeUtil;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.context.annotation.Lazy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "participant_genomic_status_batch")
@Data
public class ParticipantGenomicStatusBatch extends IdGeneratorAbstract {

    private static final long serialVersionUID = 5640737975537993933L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "IdOrGenerated")
    @GenericGenerator(name = "IdOrGenerated", strategy = "com.vibrent.drc.domain.UseIdOrGenerate")
    private Long id;


    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "batch_payload", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String batchPayload;

    @Column(name = "status", nullable = false)
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private ExternalGenomicPayloadProcessingStatus status;

    @ManyToOne
    @Lazy
    @JoinColumn(name = "participant_genomic_status_payload_id")
    private ParticipantGenomicStatusPayload participantGenomicStatusPayload;

    @Column(name = "retry_count")
    protected Integer retryCount;

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

    public ParticipantGenomicStatusBatch() {
    }

    public ParticipantGenomicStatusBatch(ParticipantGenomicStatusBatch participantGenomicStatusBatch) {
        this.id = participantGenomicStatusBatch.id;
        this.status = participantGenomicStatusBatch.status;
        this.batchPayload = participantGenomicStatusBatch.batchPayload;
        this.batchSize = participantGenomicStatusBatch.batchSize;
        this.retryCount = participantGenomicStatusBatch.retryCount;
        this.updatedOn = participantGenomicStatusBatch.updatedOn;
        this.createdOn = participantGenomicStatusBatch.createdOn;
        this.participantGenomicStatusPayload = participantGenomicStatusBatch.participantGenomicStatusPayload;
    }

    public static ParticipantGenomicStatusBatch newInstance(ParticipantGenomicStatusBatch participantGenomicStatusBatch) {
        return new ParticipantGenomicStatusBatch(participantGenomicStatusBatch);
    }
}
