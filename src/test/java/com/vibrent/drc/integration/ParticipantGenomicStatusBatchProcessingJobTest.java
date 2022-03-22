package com.vibrent.drc.integration;

import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.domain.ParticipantGenomicStatusPayload;
import com.vibrent.drc.enumeration.ExternalGenomicPayloadProcessingStatus;
import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.repository.ParticipantGenomicStatusBatchRepository;
import com.vibrent.drc.repository.ParticipantGenomicStatusPayloadRepository;
import com.vibrent.drc.scheduling.ParticipantGenomicStatusBatchProcessingJob;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.ParticipantGenomicStatusBatchProcessingService;
import com.vibrent.drc.service.ParticipantGenomicsStatusPayloadMapper;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.service.impl.ParticipantGenomicsStatusPayloadMapperImpl;
import com.vibrent.vxp.push.DRCExternalEventDto;
import io.micrometer.core.instrument.Counter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.Mockito.*;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
public class ParticipantGenomicStatusBatchProcessingJobTest extends IntegrationTest {

    ParticipantGenomicStatusBatchProcessingJob participantGenomicStatusBatchProcessingJob;
    private static final String VALID_RESPONSE = "{\"data\":[{\"module\":\"gem\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12345\"},{\"module\":\"pgx\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12348\"},{\"module\":\"pgx\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12349\"},{\"module\":\"hdr\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12350\"},{\"module\":\"hdr\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12351\"},{\"module\":\"hdr\",\"type\":\"appointment\",\"status\":\"completed\",\"appointment_id\":\"124\",\"participant_id\":\"P12354\"}],\"timestamp\":\"2020-03-18T08:02:25-05:00\"}";
    private static final String BATCH_PAYLOAD = "[\r\n    {\r\n        \"participant_id\": \"P12345\",\r\n        \"status\": \"ready\",\r\n        \"module\": \"gem\",\r\n        \"type\": \"informingLoop\"\r\n    },\r\n    {\r\n        \"participant_id\": \"P12346\",\r\n        \"status\": \"completed\",\r\n        \"module\": \"gem\",\r\n        \"type\": \"informingLoop\"\r\n    },\r\n    {\r\n        \"participant_id\": \"P12354\",\r\n        \"status\": \"completed\",\r\n        \"module\": \"hdr\",\r\n        \"type\": \"appointment\",\r\n        \"appointment_id\": \"124\"\r\n    }\r\n]";

    @Autowired
    private ParticipantGenomicStatusBatchProcessingService participantGenomicStatusBatchProcessingService;

    @Mock
    private DrcExternalEventProducer drcExternalEventProducer;

    @Autowired
    private ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper;

    @Autowired
    private ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository;

    @Autowired
    private ParticipantGenomicStatusPayloadRepository participantGenomicStatusPayloadRepository;

    @Mock
    private ParticipantService participantService;

    @Mock
    JobExecutionContext context;

    @MockBean
    @Qualifier("realTimeApiInitiatedCounter")
    private  Counter realTimeApiInitiatedCounter;

    @MockBean
    @Qualifier("realTimeApiInvokedSuccessfullyCounter")
    private  Counter realTimeApiInvokedSuccessfullyCounter;

    @MockBean
    @Qualifier("participantLookupApiInitiatedCounter")
    private  Counter participantLookupApiInitiatedCounter;

    @MockBean
    @Qualifier("participantLookupApiInvokedSuccessfullyCounter")
    private  Counter participantLookupApiInvokedSuccessfullyCounter;

    @MockBean
    @Qualifier("genomicsStatusFetchInitiatedCounter")
    private  Counter genomicsStatusFetchInitiatedCounter;

    @MockBean
    @Qualifier("genomicsStatusMessagesSentCounter")
    private  Counter genomicsStatusMessagesSentCounter;

    @MockBean
    @Qualifier("genomicsStatusProcessingFailureCounter")
    private  Counter genomicsStatusProcessingFailureCounter;


    @Mock
    DataSharingMetricsService dataSharingMetricsService;

    @Before
    public void setUp() {
       // dataSharingMetricsService = new DataSharingMetricsServiceImpl(realTimeApiInitiatedCounter, realTimeApiInvokedSuccessfullyCounter, participantLookupApiInitiatedCounter, participantLookupApiInvokedSuccessfullyCounter, genomicsStatusFetchInitiatedCounter, genomicsStatusMessagesSentCounter, genomicsStatusProcessingFailureCounter);
        participantGenomicsStatusPayloadMapper = new ParticipantGenomicsStatusPayloadMapperImpl(participantService, dataSharingMetricsService);
        participantGenomicStatusBatchProcessingJob = new ParticipantGenomicStatusBatchProcessingJob(participantGenomicStatusBatchProcessingService, drcExternalEventProducer, participantGenomicsStatusPayloadMapper, dataSharingMetricsService);

    }

    @DisplayName("When Participant Genomic Status Batch Processing Job executed, " +
            "Then verify kafka message sent for each payload." +
            "And batch status updated PENDING to PROGRESSING")
    @Test
    public void whenJobExecutesThenVerifyEligibleBatchesGetProcessed() throws JobExecutionException {

        when(participantService.getVibrentId("P12345")).thenReturn(12345L);
        when(participantService.getVibrentId("P12346")).thenReturn(12346L);
        when(participantService.getVibrentId("P12354")).thenReturn(12354L);
        addParticipantBatch(ExternalGenomicPayloadProcessingStatus.PENDING);

        //Execute Job
        participantGenomicStatusBatchProcessingJob.execute(context);

        //Verify kafka message sent
        verify(drcExternalEventProducer, times(3)).send(any(DRCExternalEventDto.class));

        //Verify Status updated to PROCESSING
        List<ParticipantGenomicStatusBatch> statusBatches = participantGenomicStatusBatchRepository.findAll();
        Assert.assertEquals(ExternalGenomicPayloadProcessingStatus.PROCESSING, statusBatches.get(0).getStatus());

        verify(dataSharingMetricsService, times(3)).incrementGenomicsStatusMessagesSentCounter();
    }

    @DisplayName("When Participant Genomic Status Batch Processing Job executed, " +
            "Then verify kafka message sent for each payload." +
            "And batch status updated from ERROR to PROGRESSING")
    @Test
    public void whenJobExecutesThenVerifyEligibleBatchesGetWithStatusErrorGetProcessed() throws JobExecutionException {

        when(participantService.getVibrentId("P12345")).thenReturn(12345L);
        when(participantService.getVibrentId("P12346")).thenReturn(12346L);
        when(participantService.getVibrentId("P12354")).thenReturn(12354L);
        addParticipantBatch(ExternalGenomicPayloadProcessingStatus.ERROR);

        //Execute Job
        participantGenomicStatusBatchProcessingJob.execute(context);

        //Verify kafka message sent
        verify(drcExternalEventProducer, times(3)).send(any(DRCExternalEventDto.class));

        //Verify Status updated to PROCESSING
        List<ParticipantGenomicStatusBatch> statusBatches = participantGenomicStatusBatchRepository.findAll();
        Assert.assertEquals(ExternalGenomicPayloadProcessingStatus.PROCESSING, statusBatches.get(0).getStatus());
    }
    @DisplayName("When database not have batch with status PENDING or ERROR" +
            "Then Verify Participant Genomic Status Batch Processing Job Not executed, " +
            "And batch status is not updated")
    @Test
    public void whenJobExecutesThenVerifyIfEligibleBatchesNotPresentThenJobWontExecute() throws JobExecutionException {
        addParticipantBatch(ExternalGenomicPayloadProcessingStatus.PROCESSING);
        addParticipantBatch(ExternalGenomicPayloadProcessingStatus.COMPLETE);

        //Execute Job
        participantGenomicStatusBatchProcessingJob.execute(context);

        //Verify kafka message not sent
        verify(drcExternalEventProducer, times(0)).send(any(DRCExternalEventDto.class));
        List<ParticipantGenomicStatusBatch> statusBatches = participantGenomicStatusBatchRepository.findAll();
        //Verify Status not updated
        Assert.assertEquals(ExternalGenomicPayloadProcessingStatus.PROCESSING, statusBatches.get(0).getStatus());
        Assert.assertEquals(ExternalGenomicPayloadProcessingStatus.COMPLETE, statusBatches.get(1).getStatus());
    }


    // Helper methods
    public void addParticipantBatch(ExternalGenomicPayloadProcessingStatus status) {
        ParticipantGenomicStatusPayload participantGenomicStatusPayload = new ParticipantGenomicStatusPayload();
        participantGenomicStatusPayload.setRawPayload(VALID_RESPONSE);
        participantGenomicStatusPayload.setStatus(ExternalGenomicPayloadProcessingStatus.PENDING);
        participantGenomicStatusPayload.setNextTimestamp("2020-03-18T08:02:25-05:00");
        participantGenomicStatusPayload.setRequestedTimestamp("2020-03-18T08:02:25-05:00");
        participantGenomicStatusPayloadRepository.save(participantGenomicStatusPayload);

        ParticipantGenomicStatusBatch participantGenomicStatusBatch = new ParticipantGenomicStatusBatch();
        participantGenomicStatusBatch.setStatus(status);
        participantGenomicStatusBatch.setBatchSize(100);
        participantGenomicStatusBatch.setRetryCount(1);
        participantGenomicStatusBatch.setBatchPayload(BATCH_PAYLOAD);
        participantGenomicStatusBatch.setParticipantGenomicStatusPayload(participantGenomicStatusPayload);
        participantGenomicStatusBatchRepository.save(participantGenomicStatusBatch);
    }

}
