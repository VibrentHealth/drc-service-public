package com.vibrent.drc.configuration;

import com.vibrent.drc.cache.VibrentIdCacheManager;
import com.vibrenthealth.drcutils.connector.DRCServiceAccountConnector;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import com.vibrenthealth.drcutils.service.impl.DRCBackendProcessorServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCConfigServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Profile("test")
@Configuration
@ComponentScan
public class BeanConfiguration {

    @Bean
    @Primary
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public DRCConfigServiceImpl drcConfigService() {
        return Mockito.mock(DRCConfigServiceImpl.class);
    }

    @Bean
    @Primary
    public DRCServiceAccountConnector drcServiceAccountConnector() {
        return Mockito.mock(DRCServiceAccountConnector.class);
    }

    @Bean
    public DRCRetryService drcRetryService(@Value("${vibrent.drc.postprocess}") boolean runPostProcessing,
                                           @Value("${vibrent.drc.apiUrl}") String drcApiBaseUrl) {
        return Mockito.mock(DRCRetryServiceImpl.class);
    }

    @Bean
    @Primary
    public DRCBackendProcessorServiceImpl drcBackendProcessorService() {
        return Mockito.mock(DRCBackendProcessorServiceImpl.class);
    }

    @Bean
    @Primary
    public VibrentIdCacheManager vibrentIdCacheManager() {
        return Mockito.mock(VibrentIdCacheManager.class);
    }

    @Bean("realTimeApiInitiatedCounter")
    public Counter realTimeApiInitiatedCounter() {
        return Counter.builder("real_time_api_call_initiated")
                .tag("type", "real_time_api_call_initiated")
                .description("Total number calls made to real time API")
                .register(meterRegistry());
    }

    @Bean("realTimeApiInvokedSuccessfullyCounter")
    public Counter realTimeApiInvokedSuccessfullyCounter() {
        return Counter.builder("real_time_api_call_invoked_successfully")
                .tag("type", "real_time_api_call_invoked_successfully")
                .description("Call to Real time API invoked successfully")
                .register(meterRegistry());
    }


    @Bean("participantLookupApiInitiatedCounter")
    public Counter participantLookupApiInitiatedCounter() {
        return Counter.builder("participant_lookup_api_call_initiated")
                .tag("type", "participant_lookup_api_call_initiated")
                .description("Total number calls made to Participant Lookup api")
                .register(meterRegistry());
    }

    @Bean("participantLookupApiInvokedSuccessfullyCounter")
    public Counter participantLookupApiCallFailedCounter() {
        return Counter.builder("participant_lookup_api_call_invoked_successfully")
                .tag("type", "participant_lookup_api_call_invoked_successfully")
                .description("Call to Participant Lookup API invoked successfully")
                .register(meterRegistry());
    }


    @Bean("genomicsStatusFetchInitiatedCounter")
    public Counter genomicsStatusFetchInitiatedCounter() {
        return Counter.builder("number_of_genomics_status_fetched")
                .tag("type", "number_of_genomics_status_fetched")
                .description("Number of Genomics status fetched in the daily Job")
                .register(meterRegistry());
    }

    @Bean("genomicsStatusMessagesSentCounter")
    public Counter genomicsStatusMessagesSentCounter() {
        return Counter.builder("number_of_genomics_status_messages_sent")
                .tag("type", "number_of_genomics_status_messages_sent")
                .description("Number of Genomics status external event sent")
                .register(meterRegistry());
    }

    @Bean("genomicsStatusProcessingFailureCounter")
    public Counter genomicsStatusProcessingFailureCounter() {
        return Counter.builder("failure_while_processing_genomics_status")
                .tag("type", "failure_while_processing_genomics_status")
                .description("Failure occurs while Genomics status processing")
                .register(meterRegistry());
    }

}
