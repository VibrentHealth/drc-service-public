package com.vibrent.drc.repository;

import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Stream;

public interface ParticipantGenomicStatusBatchRepository extends JpaRepository<ParticipantGenomicStatusBatch, Long> {
    Stream<ParticipantGenomicStatusBatch> findByStatusIn(List<ExternalGenomicPayloadProcessingStatus> pendingRetryStatusList);
}
