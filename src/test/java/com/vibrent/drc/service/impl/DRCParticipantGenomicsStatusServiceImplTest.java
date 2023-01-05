package com.vibrent.drc.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import com.vibrent.drc.repository.ParticipantGenomicStatusBatchRepository;
import com.vibrent.drc.repository.ParticipantGenomicStatusPayloadRepository;
import com.vibrent.drc.repository.SystemPropertiesRepository;
import com.vibrent.drc.service.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import io.micrometer.core.instrument.Counter;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@RunWith(MockitoJUnitRunner.class)
public class DRCParticipantGenomicsStatusServiceImplTest {

    private static final String DEFAULT_START_DATE = "1970-01-01T00:00:00-00:00";

    private final String VALID_RESPONSE = "{\"data\":[{\"module\":\"gem\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12345\"},{\"module\":\"pgx\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12348\"},{\"module\":\"pgx\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12349\"},{\"module\":\"hdr\",\"type\":\"informingLoop\",\"status\":\"ready\",\"participant_id\":\"P12350\"},{\"module\":\"hdr\",\"type\":\"result\",\"status\":\"ready\",\"participant_id\":\"P12351\"},{\"module\":\"hdr\",\"type\":\"appointment\",\"status\":\"completed\",\"appointment_id\":\"124\",\"participant_id\":\"P12354\"}],\"timestamp\":\"2020-03-18T08:02:25-05:00\"}";
    private final String SCHEDULING_VALID_RESPONSE = "{\"data\":[{\"type\":\"appointment\",\"module\":\"hdr\",\"status\":\"scheduled\",\"appointment_id\":32,\"note_available\":false,\"participant_id\":\"P658927568\"},{\"type\":\"appointment\",\"module\":\"hdr\",\"status\":\"scheduled\",\"location\":\"CA\",\"appointment_id\":32,\"note_available\":false,\"participant_id\":\"P658927568\"},{\"type\":\"appointment\",\"module\":\"hdr\",\"status\":\"scheduled\",\"appointment_id\":32,\"contact_number\":\"17033011116\",\"note_available\":false,\"participant_id\":\"P658927568\"},{\"type\":\"appointment\",\"module\":\"hdr\",\"source\":\"Color\",\"status\":\"scheduled\",\"appointment_id\":33,\"note_available\":false,\"participant_id\":\"P887038428\",\"appointment_timezone\":\"America/Los_Angeles\"}],\"timestamp\":\"2022-09-26T07:58:37.785398+00:00\"}";
    private final String INVALID_RESPONSE = null;
    private static final String BASE_URL = "https://pmi-drc-api-test.appspot.com/rdr/v2";
    public static final String INTERNAL_ID = "internalId";
    public static final String EXTERNAL_ID = "externalId";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_DESCRIPTION = "eventDescription";
    public static final String URL_GENOMICS_REPORT_READY_STATUS = "/GenomicOutreachV2";


    @Mock
    private SystemPropertiesRepository systemPropertiesRepository;

    @Mock
    private DRCBackendProcessorService drcBackendProcessorService;

    @Mock
    private DRCConfigService drcConfigService;

    @Mock
    private ParticipantGenomicStatusPayloadRepository participantGenomicStatusPayloadRepository;

    @Mock
    private ParticipantGenomicStatusBatchRepository participantGenomicStatusBatchRepository;

    @Mock
    private ParticipantGenomicsStatusPayloadMapper participantGenomicsStatusPayloadMapper;

    @Mock
    private  Counter realTimeApiInitiatedCounter;

    @Mock
    private  Counter realTimeApiInvokedSuccessfullyCounter;

    @Mock
    private  Counter participantLookupApiInitiatedCounter;

    @Mock
    private  Counter participantLookupApiInvokedSuccessfullyCounter;


    @Mock
    private  Counter genomicsStatusFetchInitiatedCounter;

    @Mock
    private  Counter genomicsStatusMessagesSentCounter;

    @Mock
    private  Counter genomicsStatusProcessingFailureCounter;

    @Mock
    ExternalApiRequestLogsService externalApiRequestLogsService;

    DataSharingMetricsService dataSharingMetricsService;

    private DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService;

    DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    @Before
    public void setUp() throws Exception {
        dataSharingMetricsService = new DataSharingMetricsServiceImpl(realTimeApiInitiatedCounter, realTimeApiInvokedSuccessfullyCounter, participantLookupApiInitiatedCounter, participantLookupApiInvokedSuccessfullyCounter, genomicsStatusFetchInitiatedCounter, genomicsStatusMessagesSentCounter, genomicsStatusProcessingFailureCounter);
        drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);

        drcParticipantGenomicsStatusService = new DRCParticipantGenomicsStatusServiceImpl(systemPropertiesRepository, drcConfigService, participantGenomicStatusPayloadRepository, participantGenomicStatusBatchRepository, participantGenomicsStatusPayloadMapper, dataSharingMetricsService, drcBackendProcessorWrapper);
        ReflectionTestUtils.setField(drcParticipantGenomicsStatusService, "batchProcessingSize", 10);
    }

    @DisplayName("When DRC is not initialised, Then verify DRC call is not triggered.")
    @Test
    public void test_when_DrcIsNotInitialised_Then_verifyNoCallTo_DRCIsMade() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(false);
        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS);
        Mockito.verify(drcBackendProcessorService, Mockito.times(0)).sendRequest(uriString, null, RequestMethod.GET, null);

    }

    @DisplayName("When DRC is initialised, Then verify DRC call is triggered.")
    @Test
    public void test_whenDrcIsInitialised_Then_verifyCallToSaveDataIsMade() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcResponse(VALID_RESPONSE));

        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS );

        Mockito.verify(drcBackendProcessorService, Mockito.times(1)).sendRequest(uriString, null, RequestMethod.GET, null);
    }


    @DisplayName("When Success response is not received from DRC , Then verify warning is logged.")
    @Test
    public void test_whenDrcIsInitialised_AndSuccessResponseIsNotReceived() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcFailureResponse());
        Logger logger = (Logger) LoggerFactory.getLogger(DRCParticipantGenomicsStatusServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS );
        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertEquals("Drc-Service : Error response received from DRC with status code as {} while calling {}", logsList.get(1).getMessage());
    }


    @DisplayName("When DRC send is successful, Then verify external event log is send.")
    @Test
    public void testWhenDrcSendSuccessfulThenVerifyExternalEventLogIsSend() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcResponse(VALID_RESPONSE));

        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS );

        ArgumentCaptor<ExternalApiRequestLog> externalApiRequestLogArgumentCaptor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        Mockito.verify(drcBackendProcessorService, Mockito.times(1)).sendRequest(uriString, null, RequestMethod.GET, null);
        Mockito.verify(externalApiRequestLogsService, Mockito.times(1)).send(externalApiRequestLogArgumentCaptor.capture());

        ExternalApiRequestLog actual = externalApiRequestLogArgumentCaptor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals(uriString , actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNull(actual.getRequestBody());
        assertEquals(VALID_RESPONSE, actual.getResponseBody());
        assertEquals(200 , actual.getResponseCode());
        assertNotEquals(0 , actual.getRequestTimestamp());
        assertNotEquals(0 , actual.getResponseTimestamp());
        assertNull(actual.getInternalId());
        assertNull(actual.getExternalId());
        assertEquals(ExternalEventType.DRC_GENOMICS_RESULT_STATUS, actual.getEventType());
        assertNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE , actual.getEventSource());
    }


    @DisplayName("When DRC send genomic scheduling status is successful, Then verify external event log is send.")
    @Test
    public void testWhenDrcSendSchedulingStatusSuccessfulThenVerifyExternalEventLogIsSend() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(DrcConstant.URL_GENOMICS_PARTICIPANT_SCHEDULING);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcResponse(SCHEDULING_VALID_RESPONSE));

        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_SCHEDULING, ExternalEventType.DRC_GENOMICS_SCHEDULING_STATUS, SystemPropertiesEnum.DRC_GENOMICS_SCHEDULING_STATUS);

        ArgumentCaptor<ExternalApiRequestLog> externalApiRequestLogArgumentCaptor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        Mockito.verify(drcBackendProcessorService, Mockito.times(1)).sendRequest(uriString, null, RequestMethod.GET, null);
        Mockito.verify(externalApiRequestLogsService, Mockito.times(1)).send(externalApiRequestLogArgumentCaptor.capture());

        ExternalApiRequestLog actual = externalApiRequestLogArgumentCaptor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals(uriString , actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNull(actual.getRequestBody());
        assertEquals(SCHEDULING_VALID_RESPONSE, actual.getResponseBody());
        assertEquals(200 , actual.getResponseCode());
        assertNotEquals(0 , actual.getRequestTimestamp());
        assertNotEquals(0 , actual.getResponseTimestamp());
        assertNull(actual.getInternalId());
        assertNull(actual.getExternalId());
        assertEquals(ExternalEventType.DRC_GENOMICS_SCHEDULING_STATUS, actual.getEventType());
        assertNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE , actual.getEventSource());
    }


    @DisplayName("When DRC send failed, then verify external log is send with error response")
    @Test
    public void whenDRCSendFailedThenVerifyExternalLogIsSendWithErrorResponse() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.when(drcBackendProcessorService.sendRequest(uriString, null, RequestMethod.GET, null)).thenReturn(getDrcFailureResponse());

        drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS );
        ArgumentCaptor<ExternalApiRequestLog> externalApiRequestLogArgumentCaptor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        Mockito.verify(drcBackendProcessorService, Mockito.times(1)).sendRequest(uriString, null, RequestMethod.GET, null);
        Mockito.verify(externalApiRequestLogsService, Mockito.times(1)).send(externalApiRequestLogArgumentCaptor.capture());

        ExternalApiRequestLog actual = externalApiRequestLogArgumentCaptor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals(uriString , actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNull(actual.getRequestBody());
        assertEquals(INVALID_RESPONSE, actual.getResponseBody());
        assertEquals(400 , actual.getResponseCode());
        assertNotEquals(0 , actual.getRequestTimestamp());
        assertNotEquals(0 , actual.getResponseTimestamp());
        assertNull(actual.getInternalId());
        assertNull(actual.getExternalId());
        assertEquals(ExternalEventType.DRC_GENOMICS_RESULT_STATUS, actual.getEventType());
        assertNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE , actual.getEventSource());
    }


    @DisplayName("When DRC send failed, then verify external log is send with error response")
    @Test
    public void whenDRCSendRequestThrowsDrcConnectorExceptionThenVerifyExternalLogIsSendWithErrorResponse() throws DrcConnectorException, JsonProcessingException {
        String uriString = getUriString(URL_GENOMICS_REPORT_READY_STATUS);

        Mockito.when(drcConfigService.getDrcApiBaseUrl()).thenReturn(BASE_URL);
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        Mockito.doThrow(new DrcConnectorException("error", new HttpResponseException.Builder(400, "400 Bad Request", new HttpHeaders()).build())).when(drcBackendProcessorService).sendRequest(uriString, null, RequestMethod.GET, null);

        try {
            drcParticipantGenomicsStatusService.retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS);
        } catch (DrcConnectorException ignored) {
        }

        ArgumentCaptor<ExternalApiRequestLog> externalApiRequestLogArgumentCaptor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        Mockito.verify(drcBackendProcessorService, Mockito.times(1)).sendRequest(uriString, null, RequestMethod.GET, null);
        Mockito.verify(externalApiRequestLogsService, Mockito.times(1)).send(externalApiRequestLogArgumentCaptor.capture());

        ExternalApiRequestLog actual = externalApiRequestLogArgumentCaptor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals(uriString , actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(400, actual.getResponseCode());
        assertNotEquals(0 , actual.getRequestTimestamp());
        assertNotEquals(0 , actual.getResponseTimestamp());
        assertNull(actual.getInternalId());
        assertNull(actual.getExternalId());
        assertEquals(ExternalEventType.DRC_GENOMICS_RESULT_STATUS, actual.getEventType());
        assertNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE , actual.getEventSource());
    }


    // Helper methods
    public HttpResponseWrapper getDrcResponse(String response) {
        return new HttpResponseWrapper(200, response);
    }

    public HttpResponseWrapper getDrcFailureResponse() {
        return new HttpResponseWrapper(400, INVALID_RESPONSE);
    }


    private String getUriString(String url) {
        return BASE_URL +
                UriComponentsBuilder.fromUriString(url)
                        .queryParam("start_date", "{startDate}")
                        .encode()
                        .buildAndExpand(DEFAULT_START_DATE)
                        .toUriString();
    }


}