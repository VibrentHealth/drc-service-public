package com.vibrent.drc.scheduling;

import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import com.vibrent.drc.service.DRCParticipantGenomicsStatusService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DRCParticipantGenomicsStatusFetchJob implements Job {

    private final DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService;
    private final boolean genomicSchedulingWorkflow;

    public DRCParticipantGenomicsStatusFetchJob(DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService,
                                                @Value("${vibrent.drc.genomicSchedulingWorkflow.enabled}") boolean genomicSchedulingWorkflow) {
        this.drcParticipantGenomicsStatusService = drcParticipantGenomicsStatusService;
        this.genomicSchedulingWorkflow = genomicSchedulingWorkflow;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long startTime = System.currentTimeMillis();
        log.info("DRC: Started Executing DRCParticipantGenomicsStatusFetchJob Start time: {}", startTime);
        try {
            drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS );
            if (genomicSchedulingWorkflow) {
                drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_SCHEDULING, ExternalEventType.DRC_GENOMICS_SCHEDULING_STATUS, SystemPropertiesEnum.DRC_GENOMICS_SCHEDULING_STATUS);
            }
            log.info("DRC: DRCParticipantGenomicsStatusFetchJob completion time: {}, time taken for job execution {} ms", System.currentTimeMillis(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.warn("DRC-Service: Exception while fetching Genomics ReportReady / GenomicsScheduling Status from DRC ", e);
            throw new JobExecutionException("DRC: Error while fetching Genomics Report Ready Status from DRC {} ", e);
        }

    }
}
