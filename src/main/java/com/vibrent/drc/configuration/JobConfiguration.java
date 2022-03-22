package com.vibrent.drc.configuration;

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
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("DRC_Genomic_Participant_Status_Fetch_Trigger");
        CronTriggerImpl trigger = (CronTriggerImpl) scheduler.getTrigger(triggerKey);
        trigger.setCronExpression(drcProperties.getParticipantStatusFetchCron());
        scheduler.rescheduleJob(triggerKey, trigger);
        return scheduler;
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
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("DRC_Participant_Genomic_Status_Batch_Processing_Trigger");
        CronTriggerImpl trigger = (CronTriggerImpl) scheduler.getTrigger(triggerKey);
        trigger.setCronExpression(drcProperties.getParticipantBatchProcessingCron());
        scheduler.rescheduleJob(triggerKey, trigger);
        return scheduler;
    }


}
