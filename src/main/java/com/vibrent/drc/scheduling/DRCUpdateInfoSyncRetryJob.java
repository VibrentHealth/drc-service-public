package com.vibrent.drc.scheduling;

import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.service.DRCUpdateInfoSyncRetryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.stream.Stream;

@Slf4j
@Component
public class DRCUpdateInfoSyncRetryJob implements Job {

    private final DRCUpdateInfoSyncRetryService drcUpdateInfoSyncRetryService;

    public DRCUpdateInfoSyncRetryJob(DRCUpdateInfoSyncRetryService drcUpdateInfoSyncRetryService) {
        this.drcUpdateInfoSyncRetryService = drcUpdateInfoSyncRetryService;
    }

    @Transactional
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long startTime = System.currentTimeMillis();
        log.info("DRC: Started Executing retrying update info sync Start time: {}", startTime);
        try {
            Stream<DRCUpdateInfoSyncRetry> entries = drcUpdateInfoSyncRetryService.getEligibleEntries();
            entries.forEach(this::executeRetry);

            log.info("DRC: Started Executing retrying update info sync completion on: {}, time taken for job execution {} ms", System.currentTimeMillis(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.warn("DRC-Service: Exception while executing UpdateInfoSyncRetryJob", e);
        }

    }

    private void executeRetry(DRCUpdateInfoSyncRetry entry) {
        try {
        drcUpdateInfoSyncRetryService.retryUpdateInfoSync(entry);} catch (Exception e) {
            log.warn("DRC-Service: Exception while retry to send the updateInfo to drc", e);
        }
    }
}
