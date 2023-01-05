package com.vibrent.drc.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.domain.ParticipantGenomicStatusBatch;
import com.vibrent.drc.domain.ParticipantGenomicStatusPayload;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.repository.ParticipantGenomicStatusBatchRepository;
import com.vibrent.drc.repository.ParticipantGenomicStatusPayloadRepository;
import com.vibrent.drc.repository.SystemPropertiesRepository;
import com.vibrent.drc.scheduling.DRCParticipantGenomicsStatusFetchJob;
import com.vibrent.drc.service.*;
import com.vibrent.drc.service.impl.DRCBackendProcessorWrapperImpl;
import com.vibrent.drc.service.impl.DRCParticipantGenomicsStatusServiceImpl;
import com.vibrent.drc.service.impl.ParticipantServiceImpl;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
public class DRCParticipantGenomicsStatusFetchJobTest extends IntegrationTest {

    private static final String DEFAULT_START_DATE = "1970-01-01T00:00:00-00:00";
    private static final String VALID_RESPONSE = "{\"data\":[{\"module\":\"gem\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12345\"},{\"module\":\"pgx\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12348\"},{\"module\":\"pgx\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12349\"},{\"module\":\"hdr\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12350\"},{\"module\":\"hdr\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12351\"},{\"module\":\"hdr\",\"type\":\"appointment\",\"status\":\"completed\",\"appointment_id\":\"124\",\"participant_id\":\"P12354\"}],\"timestamp\":\"2020-03-18T08:02:25-05:00\"}";
    private static final String BASE_URL = "https://pmi-drc-api-test.appspot.com/rdr/v2";

    private DRCParticipantGenomicsStatusFetchJob drcParticipantGenomicsStatusFetchJob;

    private DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService;

    @Autowired
    private SystemPropertiesRepository systemPropertiesRepository;

    @MockBean
    private DRCBackendProcessorService drcBackendProcessorService;

    @MockBean
    private DRCConfigService drcConfigService;

    @Autowired
    private ParticipantGenomicStatusPayloadRepository participantGenomicStatusPayloadRepository;

    @Autowired
    private ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository;

    @Autowired
    private ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper;

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

    @Mock
    ExternalApiRequestLogsService externalApiRequestLogsService;

    DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    @Mock
    DRCParticipantGenomicsStatusFetchJob mockedDrcParticipantGenomicsStatusFetchJob;

    private  Boolean genomicSchedulingWorkflow = true;

    @Before
    public void setUp() throws Exception {
        drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);
        drcParticipantGenomicsStatusService = new DRCParticipantGenomicsStatusServiceImpl(systemPropertiesRepository, drcConfigService, participantGenomicStatusPayloadRepository, participantGenomicStatusBatchRepository, participantGenomicsStatusPayloadMapper, dataSharingMetricsService, drcBackendProcessorWrapper);
        ReflectionTestUtils.setField(drcParticipantGenomicsStatusService, "batchProcessingSize", 10);
        drcParticipantGenomicsStatusFetchJob = new DRCParticipantGenomicsStatusFetchJob(drcParticipantGenomicsStatusService, genomicSchedulingWorkflow);
    }

    @DisplayName("When DRC Report Ready Status Fetch JOB executed, " +
            "Then verify payload received from DRC is saved in database.")
    @Test
    public void whenJobExecutesThenVerifyEligibleBatchesCreated() throws JobExecutionException, DrcConnectorException {

        String uriString = BASE_URL +
                UriComponentsBuilder.fromUriString(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS)
                        .queryParam("start_date", "{startDate}")
                        .encode()
                        .buildAndExpand(DEFAULT_START_DATE)
                        .toUriString();

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcResponse());

        // Execute DRC Report Ready Status Fetch job
        drcParticipantGenomicsStatusFetchJob.execute(context);

        // Verify External Report Payloads saved in database
        List<ParticipantGenomicStatusPayload> participantGenomicStatusPayloadList = participantGenomicStatusPayloadRepository.findAll();
        Assert.assertEquals(1, participantGenomicStatusPayloadList.size());


        // Verify External Report Batches saved in database
        List<ParticipantGenomicStatusBatch> participantGenomicStatusBatchList = participantGenomicStatusBatchRepository.findAll();
        Assert.assertEquals(1, participantGenomicStatusBatchList.size());

        verify(dataSharingMetricsService, times(1)).incrementGenomicsStatusFetchInitiatedCounter(6);
    }

    @DisplayName("When DRC Report Ready Status Fetch JOB executed," +
            "And encountered any exception " +
            "Then verify Error logged.")
    @Test
    public void whenJobExecutesAndEncountersExceptionThenVerifyJobExecutionExceptionThrown() throws JobExecutionException, DrcConnectorException {
        Logger logger = (Logger) LoggerFactory.getLogger(DRCParticipantGenomicsStatusServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);

        String uriString = BASE_URL +
                UriComponentsBuilder.fromUriString(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS)
                        .queryParam("start_date", "{startDate}")
                        .encode()
                        .buildAndExpand(DEFAULT_START_DATE)
                        .toUriString();

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcResponse());
        doThrow(TimeoutException.class).when(externalApiRequestLogsService).send(any(ExternalApiRequestLog.class));

        listAppender.start();
        drcParticipantGenomicsStatusFetchJob.execute(context);
        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals("ERROR", logsList.get(1).getLevel().toString());
        assertEquals("DRC-Service: Exception while fetching {} from DRC ", logsList.get(1).getMessage());


    }


    //Helper methods

    public HttpResponseWrapper getDrcResponse() {
        HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper(200, VALID_RESPONSE);
        return httpResponseWrapper;
    }
}
