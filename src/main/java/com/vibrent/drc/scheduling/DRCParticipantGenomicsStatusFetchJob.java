package com.vibrent.drc.scheduling;

import com.vibrent.drc.service.DRCParticipantGenomicsStatusService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DRCParticipantGenomicsStatusFetchJob implements Job {

    private final DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService;

    public DRCParticipantGenomicsStatusFetchJob(DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService) {
        this.drcParticipantGenomicsStatusService = drcParticipantGenomicsStatusService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long startTime = System.currentTimeMillis();
        log.info("DRC: Started Executing DRCParticipantGenomicsStatusFetchJob Start time: {}", startTime);
        try {
            drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc();
            log.info("DRC: DRCParticipantGenomicsStatusFetchJob completion time: {}, time taken for job execution {} ms", System.currentTimeMillis() ,System.currentTimeMillis()-startTime);
        } catch (Exception e) {
            log.warn("DRC-Service: Exception while fetching Genomics Report Ready Status from DRC ", e);
            throw new JobExecutionException("DRC: Error while fetching Genomics Report Ready Status from DRC {} ", e);
        }

    }
}
