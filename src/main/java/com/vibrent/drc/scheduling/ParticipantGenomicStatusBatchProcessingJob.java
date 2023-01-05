package com.vibrent.drc.scheduling;

import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.ParticipantGenomicStatusBatchProcessingService;
import com.vibrent.drc.service.ParticipantGenomicsStatusPayloadMapper;
import com.vibrent.vxp.push.DRCExternalEventDto;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ParticipantGenomicStatusBatchProcessingJob implements Job {

    private final ParticipantGenomicStatusBatchProcessingService participantGenomicStatusBatchProcessingService;
    private final DrcExternalEventProducer drcExternalEventProducer;
    private final ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper;
    private final DataSharingMetricsService dataSharingMetricsService;

    public ParticipantGenomicStatusBatchProcessingJob(ParticipantGenomicStatusBatchProcessingService participantGenomicStatusBatchProcessingService, DrcExternalEventProducer drcExternalEventProducer, ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper,
                                                      DataSharingMetricsService dataSharingMetricsService) {
        this.participantGenomicStatusBatchProcessingService = participantGenomicStatusBatchProcessingService;
        this.drcExternalEventProducer = drcExternalEventProducer;
        this.participantGenomicsStatusPayloadMapper = participantGenomicsStatusPayloadMapper;
        this.dataSharingMetricsService = dataSharingMetricsService;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long startTime = System.currentTimeMillis();
        log.info("DRC: Executing ParticipantGenomicStatusBatchProcessingJob start time: {}", startTime);
        Stream<ParticipantGenomicStatusBatch> eligibleGenomicStatusBatches = participantGenomicStatusBatchProcessingService.getEligibleGenomicStatusBatches();
        eligibleGenomicStatusBatches
                .forEach(
                        genomicStatusBatch -> {
                            try {
                                List<DRCExternalEventDto> drcExternalEventDtoList = participantGenomicsStatusPayloadMapper.mapJsonStringToDrcExternalEventDto(genomicStatusBatch.getBatchPayload());
                                for (DRCExternalEventDto dto : drcExternalEventDtoList) {
                                    drcExternalEventProducer.send(dto);
                                    dataSharingMetricsService.incrementGenomicsStatusMessagesSentCounter();
                                }

                                participantGenomicStatusBatchProcessingService.updateBatchStatus(ParticipantGenomicStatusBatch.newInstance(genomicStatusBatch), ExternalGenomicPayloadProcessingStatus.PROCESSING);
                            } catch (IOException e) {
                                log.error("DRC: Exception while converting DRC payload: {}", e);
                            }
                        }
                );

        log.info("DRC: Complete time taken to execute ParticipantGenomicStatusBatchProcessingJob : {} ms", System.currentTimeMillis() - startTime);
    }
}