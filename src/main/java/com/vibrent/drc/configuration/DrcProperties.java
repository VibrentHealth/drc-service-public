package com.vibrent.drc.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties
public class DrcProperties {

    @Value("${vibrent.drc.genomics.participantStatus.cron}")
    private String participantStatusFetchCron;

    @Value("${vibrent.drc.genomics.participantBatch.cron}")
    private String participantBatchProcessingCron;

    @Value("${vibrent.drc.sync.retry.cron}")
    private String drcSyncRetryCron;

    @Value("${vibrent.drc.apiUrl}")
    private String drcApiBaseUrl;

    @Value("${vibrent.drc.forms.basics.name}")
    private String basicsFormName;

    @Value("${vibrent.drc.forms.basics.id}")
    private Long basicsFormId;

}
