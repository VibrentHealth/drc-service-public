package com.vibrent.drc.service;

public interface DataSharingMetricsService {
    /**
     * Increment the counter when realtime endpoint is called
     */
    void incrementRealTimeApiInitiatedCounter();

    /**
     * Increment the counter when pid reconciliation endpoint is called
     */
    void incrementParticipantLookupApiInitiatedCounter();


    /**
     * Increment the counter when realtime endpoint is Invoked Successfully
     */
    void incrementRealTimeApiCallInvokedSuccessfullyCounter();

    /**
     * Increment the counter when pid reconciliation endpoint is  Invoked Successfully
     */
    void incrementParticipantLookupApiCallInvokedSuccessfullyCounter();

    /**
     * Increment the counter when Genomics Status Fetch Job is executed
     */
    void incrementGenomicsStatusFetchInitiatedCounter(int size);

    /**
     * Increment the counter when Genomics Status Messages Sent
     */
     void incrementGenomicsStatusMessagesSentCounter();

    /**
     * Increment the counter when Genomics Status Processing Failed
     */
    void incrementGenomicsStatusProcessingFailureCounter();

}
