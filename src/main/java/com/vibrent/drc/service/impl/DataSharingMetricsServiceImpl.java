package com.vibrent.drc.service.impl;

import com.vibrent.drc.service.DataSharingMetricsService;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DataSharingMetricsServiceImpl implements DataSharingMetricsService {

    private final Counter realTimeApiInitiatedCounter;
    private final Counter realTimeApiInvokedSuccessfullyCounter;
    private final Counter participantLookupApiInitiatedCounter;
    private final Counter participantLookupApiInvokedSuccessfullyCounter;
    private final Counter genomicsStatusFetchInitiatedCounter;
    private final Counter genomicsStatusMessagesSentCounter;
    private final Counter genomicsStatusProcessingFailureCounter;

    public DataSharingMetricsServiceImpl(@Qualifier("realTimeApiInitiatedCounter") Counter realTimeApiInitiatedCounter,
                                         @Qualifier("realTimeApiInvokedSuccessfullyCounter") Counter realTimeApiInvokedSuccessfullyCounter,
                                         @Qualifier("participantLookupApiInitiatedCounter") Counter participantLookupApiInitiatedCounter,
                                         @Qualifier("participantLookupApiInvokedSuccessfullyCounter") Counter participantLookupApiInvokedSuccessfullyCounter,
                                         @Qualifier("genomicsStatusFetchInitiatedCounter") Counter genomicsStatusFetchInitiatedCounter,
                                         @Qualifier("genomicsStatusMessagesSentCounter") Counter genomicsStatusMessagesSentCounter,
                                         @Qualifier("genomicsStatusProcessingFailureCounter") Counter genomicsStatusProcessingFailureCounter) {
        this.realTimeApiInitiatedCounter = realTimeApiInitiatedCounter;
        this.realTimeApiInvokedSuccessfullyCounter = realTimeApiInvokedSuccessfullyCounter;
        this.participantLookupApiInitiatedCounter = participantLookupApiInitiatedCounter;
        this.participantLookupApiInvokedSuccessfullyCounter = participantLookupApiInvokedSuccessfullyCounter;
        this.genomicsStatusFetchInitiatedCounter = genomicsStatusFetchInitiatedCounter;
        this.genomicsStatusMessagesSentCounter = genomicsStatusMessagesSentCounter;
        this.genomicsStatusProcessingFailureCounter = genomicsStatusProcessingFailureCounter;
    }


    @Override
    public void incrementRealTimeApiInitiatedCounter() {
        realTimeApiInitiatedCounter.increment();
    }

    @Override
    public void incrementParticipantLookupApiInitiatedCounter() {
        participantLookupApiInitiatedCounter.increment();
    }


    @Override
    public void incrementRealTimeApiCallInvokedSuccessfullyCounter() {
        realTimeApiInvokedSuccessfullyCounter.increment();
    }


    @Override
    public void incrementParticipantLookupApiCallInvokedSuccessfullyCounter() {
        participantLookupApiInvokedSuccessfullyCounter.increment();
    }


    @Override
    public void incrementGenomicsStatusFetchInitiatedCounter(int size) {
        genomicsStatusFetchInitiatedCounter.increment(size);
    }

    @Override
    public void incrementGenomicsStatusMessagesSentCounter() {
        genomicsStatusMessagesSentCounter.increment();
    }

    @Override
    public void incrementGenomicsStatusProcessingFailureCounter() {
        genomicsStatusProcessingFailureCounter.increment();
    }


}
