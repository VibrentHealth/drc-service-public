package com.vibrent.drc.service;

import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;

import java.util.stream.Stream;

public interface ParticipantGenomicStatusBatchProcessingService {
    Stream<ParticipantGenomicStatusBatch> getEligibleGenomicStatusBatches();
    ParticipantGenomicStatusBatch updateBatchStatus(ParticipantGenomicStatusBatch participantGenomicStatusBatch, ExternalGenomicPayloadProcessingStatus status);
}
