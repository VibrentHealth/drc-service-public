package com.vibrent.drc.configuration;

import com.vibrenthealth.drcutils.connector.DRCServiceAccountConnector;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import com.vibrenthealth.drcutils.service.impl.DRCBackendProcessorServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCConfigServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public DRCConfigServiceImpl drcConfigService(@Value("${vibrent.drc.postprocess}") boolean runPostProcessing, @Value("${vibrent.drc.apiUrl}") String drcApiBaseUrl) {
        return new DRCConfigServiceImpl(runPostProcessing, drcApiBaseUrl);
    }

    @Bean
    public DRCServiceAccountConnector drcServiceAccountConnector(@Value("${vibrent.drc.drcCertificateFilePath}") String drcCertificatePath,
                                                                 @Value("${vibrent.drc.timeout}") int timeout) {
        return new DRCServiceAccountConnector(drcCertificatePath, timeout, null);

    }

    @Bean
    public DRCBackendProcessorServiceImpl drcBackendProcessorService(@Value("${vibrent.drc.drcCertificateFilePath}") String drcCertificatePath,
                                                                     @Value("${vibrent.drc.timeout}") int drcTimeout,
                                                                     @Value("${vibrent.drc.postprocess}") boolean runPostProcessing,
                                                                     @Value("${vibrent.drc.apiUrl}") String drcApiBaseUrl) {
        return new DRCBackendProcessorServiceImpl(drcServiceAccountConnector(drcCertificatePath, drcTimeout), drcConfigService(runPostProcessing, drcApiBaseUrl));
    }

    @Bean
    public DRCRetryService drcRetryService(@Value("${vibrent.drc.postprocess}") boolean runPostProcessing,
                                           @Value("${vibrent.drc.apiUrl}") String drcApiBaseUrl) {
        return new DRCRetryServiceImpl(drcConfigService(runPostProcessing, drcApiBaseUrl));
    }

    @Bean("realTimeApiInitiatedCounter")
    public Counter realTimeApiInitiatedCounter() {
        return Counter.builder("real_time_api_call_initiated")
                .tag("type", "real_time_api_call_initiated")
                .description("Total number calls made to real time API")
                .register(meterRegistry);
    }

    @Bean("realTimeApiInvokedSuccessfullyCounter")
    public Counter realTimeApiInvokedSuccessfullyCounter() {
        return Counter.builder("real_time_api_call_invoked_successfully")
                .tag("type", "real_time_api_call_invoked_successfully")
                .description("Call to Real time API invoked successfully")
                .register(meterRegistry);
    }


   @Bean("participantLookupApiInitiatedCounter")
    public Counter participantLookupApiInitiatedCounter() {
        return Counter.builder("participant_lookup_api_call_initiated")
                .tag("type", "participant_lookup_api_call_initiated")
                .description("Total number calls made to Participant Lookup api")
                .register(meterRegistry);
    }

    @Bean("participantLookupApiInvokedSuccessfullyCounter")
    public Counter participantLookupApiCallFailedCounter() {
        return Counter.builder("participant_lookup_api_call_invoked_successfully")
                .tag("type", "participant_lookup_api_call_invoked_successfully")
                .description("Call to Participant Lookup API invoked successfully")
                .register(meterRegistry);
    }


    @Bean("genomicsStatusFetchInitiatedCounter")
    public Counter genomicsStatusFetchInitiatedCounter() {
        return Counter.builder("number_of_genomics_status_fetched")
                .tag("type", "number_of_genomics_status_fetched")
                .description("Number of Genomics status fetched in the daily Job")
                .register(meterRegistry);
    }

    @Bean("genomicsStatusMessagesSentCounter")
    public Counter genomicsStatusMessagesSentCounter() {
        return Counter.builder("number_of_genomics_status_messages_sent")
                .tag("type", "number_of_genomics_status_messages_sent")
                .description("Number of Genomics status external event sent")
                .register(meterRegistry);
    }

    @Bean("genomicsStatusProcessingFailureCounter")
    public Counter genomicsStatusProcessingFailureCounter() {
        return Counter.builder("failure_while_processing_genomics_status")
                .tag("type", "failure_while_processing_genomics_status")
                .description("Failure occurs while Genomics status processing")
                .register(meterRegistry);
    }


}
