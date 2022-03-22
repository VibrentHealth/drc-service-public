package com.vibrent.drc.service.impl;

import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.repository.ParticipantGenomicStatusBatchRepository;
import com.vibrent.drc.service.ParticipantGenomicStatusBatchProcessingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ParticipantGenomicStatusBatchProcessingServiceImpl implements ParticipantGenomicStatusBatchProcessingService {

    private final ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository;

    public ParticipantGenomicStatusBatchProcessingServiceImpl(ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository) {
        this.participantGenomicStatusBatchRepository = participantGenomicStatusBatchRepository;
    }

    @Override
    public Stream<ParticipantGenomicStatusBatch> getEligibleGenomicStatusBatches() {
        List<ExternalGenomicPayloadProcessingStatus> pendingRetryStatusList = new ArrayList<>();
        pendingRetryStatusList.add(ExternalGenomicPayloadProcessingStatus.PENDING);
        pendingRetryStatusList.add(ExternalGenomicPayloadProcessingStatus.ERROR);
        return participantGenomicStatusBatchRepository.findByStatusIn(pendingRetryStatusList);
    }

    @Override
    public ParticipantGenomicStatusBatch updateBatchStatus(ParticipantGenomicStatusBatch participantGenomicStatusBatch, ExternalGenomicPayloadProcessingStatus status) {
        participantGenomicStatusBatch.setStatus(status);
        return participantGenomicStatusBatchRepository.save(participantGenomicStatusBatch);
    }
}
