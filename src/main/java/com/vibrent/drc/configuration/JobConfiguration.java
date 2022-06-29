package com.vibrent.drc.configuration;

import com.vibrent.drc.scheduling.DRCUpdateInfoSyncRetryJob;
import com.vibrent.drc.scheduling.ParticipantGenomicStatusBatchProcessingJob;
import com.vibrent.drc.scheduling.DRCParticipantGenomicsStatusFetchJob;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.text.ParseException;

@Configuration
public class JobConfiguration {

    private final DrcProperties drcProperties;

    public JobConfiguration(DrcProperties drcProperties) {
        this.drcProperties = drcProperties;
    }

    @Bean
    public JobDetail drcParticipantGenomicsStatusFetchJobDetails() {
        return JobBuilder.newJob().ofType(DRCParticipantGenomicsStatusFetchJob.class)
                .storeDurably()
                .withIdentity("DRC_Genomic_Participant_Status_Fetch_Job")
                .withDescription("Invoke DRC Participant Genomic Status Fetch Job")
                .build();
    }

    @Bean
    public Trigger drcParticipantGenomicsStatusFetchJobTrigger(JobDetail drcParticipantGenomicsStatusFetchJobDetails) {
        return TriggerBuilder.newTrigger().forJob(drcParticipantGenomicsStatusFetchJobDetails)
                .withIdentity("DRC_Genomic_Participant_Status_Fetch_Trigger")
                .withDescription("Invoke DRC Participant Status Fetch Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(drcProperties.getParticipantStatusFetchCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Scheduler rescheduleCronJob(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException, ParseException {
        return rescheduleCronJob(schedulerFactoryBean, "DRC_Genomic_Participant_Status_Fetch_Trigger", drcProperties.getParticipantStatusFetchCron());
    }

    @Bean
    public JobDetail drcParticipantGenomicStatusBatchProcessingCronJobDetails() {
        return JobBuilder.newJob().ofType(ParticipantGenomicStatusBatchProcessingJob.class)
                .storeDurably()
                .withIdentity("DRC_Participant_Genomic_Status_Batch_Processing_Job")
                .withDescription("Invoke DRC Participant Genomic Status Batch Processing Job")
                .build();
    }

    @Bean
    public Trigger drcParticipantGenomicStatusBatchProcessingJobTrigger(JobDetail drcParticipantGenomicStatusBatchProcessingCronJobDetails) {
        return TriggerBuilder.newTrigger().forJob(drcParticipantGenomicStatusBatchProcessingCronJobDetails)
                .withIdentity("DRC_Participant_Genomic_Status_Batch_Processing_Trigger")
                .withDescription("Invoke DRC Participant Genomic Status Batch Processing Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(drcProperties.getParticipantBatchProcessingCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Scheduler rescheduleGenomicStatusBatchProcessingCronJob(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException, ParseException {
        return rescheduleCronJob(schedulerFactoryBean, "DRC_Participant_Genomic_Status_Batch_Processing_Trigger", drcProperties.getParticipantBatchProcessingCron());
    }

    @Bean
    public JobDetail drcUpdateInfoSyncRetryJob() {
        return JobBuilder.newJob().ofType(DRCUpdateInfoSyncRetryJob.class)
                .storeDurably()
                .withIdentity("DRC_Update_Info_Sync_Retry_Job")
                .withDescription("Invoke DRC Sync Retry Job")
                .build();
    }

    @Bean
    public Trigger drcDrcSyncRetryQueueJobTrigger(JobDetail drcUpdateInfoSyncRetryJob) {
        return TriggerBuilder.newTrigger().forJob(drcUpdateInfoSyncRetryJob    )
                .withIdentity("DRC_Update_Info_Sync_Retry_Trigger")
                .withDescription("Invoke DRC Sync Retry Job Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(drcProperties.getParticipantStatusFetchCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Scheduler drcDrcSyncRetryQueueRescheduleCronJob(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException, ParseException {
        return rescheduleCronJob(schedulerFactoryBean, "DRC_Update_Info_Sync_Retry_Trigger", drcProperties.getDrcSyncRetryCron());
    }

    private Scheduler rescheduleCronJob(SchedulerFactoryBean schedulerFactoryBean,
                                        String triggerName,
                                        String cronExpression) throws SchedulerException, ParseException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey(triggerName);
        CronTriggerImpl trigger = (CronTriggerImpl) scheduler.getTrigger(triggerKey);
        trigger.setCronExpression(cronExpression);
        scheduler.rescheduleJob(triggerKey, trigger);
        return scheduler;
    }

}
