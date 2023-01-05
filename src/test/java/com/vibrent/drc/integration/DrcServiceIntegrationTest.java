package com.vibrent.drc.integration;

import com.vibrent.drc.exception.DrcExceptionHandler;
import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.DrcNotificationRequestService;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.RestClientUtil;
import com.vibrent.drc.web.rest.delegate.DrcServiceApiDelegate;
import com.vibrent.vxp.drc.dto.*;
import com.vibrent.vxp.drc.resource.DrcApiController;
import com.vibrent.vxp.push.DRCExternalEventDto;
import com.vibrent.vxp.push.ExternalEventSourceEnum;
import com.vibrenthealth.drcutils.service.impl.DRCConfigServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import io.micrometer.core.instrument.Counter;
import lombok.SneakyThrows;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@EnableTransactionManagement(proxyTargetClass = true)
public class DrcServiceIntegrationTest extends IntegrationTest {

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);
    public static final String POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT = "/api/v1/drc/eventNotification";
    public static final String GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT = "/api/v1/drc/participantLookup";

    @Autowired
    private DrcApiController drcApiController;

    private MockMvc mockMvc;

    @Mock
    private RestClientUtil restClientUtil;

    @Mock
    private OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;

    @MockBean
    private DrcExternalEventProducer drcExternalEventProducer;

    @Autowired
    private DrcNotificationRequestService drcNotificationRequestService;


    @Autowired
    private ParticipantService participantService;

    private OAuth2AccessToken oAuth2AccessToken;

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
    private Counter genomicsStatusFetchInitiatedCounter;

    @MockBean
    @Qualifier("genomicsStatusMessagesSentCounter")
    private  Counter genomicsStatusMessagesSentCounter;

    @MockBean
    @Qualifier("genomicsStatusProcessingFailureCounter")
    private  Counter genomicsStatusProcessingFailureCounter;

    @Mock
    DataSharingMetricsService dataSharingMetricsService;

    @Autowired
    private DrcServiceApiDelegate drcServiceApiDelegate;

    DrcNotificationRequestDTO drcNotificationRequestDTO;

    @Captor
    private ArgumentCaptor<DRCExternalEventDto> drcExternalEventDtoArgumentCaptor;

    @BeforeAll
    public static void beforeAll() {
        IntegrationTest.startAllRequired();
    }

    @Before
    public void setUp() {
        oAuth2AccessToken = new DefaultOAuth2AccessToken("access_token");
        ((DefaultOAuth2AccessToken)oAuth2AccessToken).setExpiration(Instant.now().plus(Duration.standardSeconds(300)).toDate());
        mockMvc = MockMvcBuilders.standaloneSetup(drcApiController).setControllerAdvice(DrcExceptionHandler.class).build();
        drcNotificationRequestDTO = buildDrcNotificationRequestDTO();

        ReflectionTestUtils.setField(participantService, "keycloakDrcInternalCredentialsRestTemplate", keycloakDrcInternalCredentialsRestTemplate);
        ReflectionTestUtils.setField(participantService, "retryService", new DRCRetryServiceImpl(new DRCConfigServiceImpl(false, "")));
        ReflectionTestUtils.setField(participantService, "restClientUtil", restClientUtil);
        ReflectionTestUtils.setField(drcServiceApiDelegate, "dataSharingMetricsService", dataSharingMetricsService);

    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRCService request Endpoint invoked  with INFORMING_LOOP_STARTED request object" +
            "Then verify success received")
    public void whenDrcApiIsInvokedForInformingLoopStartedThenGetSuccessHttpStatus() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"P10100\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                        .content(JacksonUtil.getMapper().writeValueAsBytes(drcNotificationRequestDTO)))
                .andExpect(status().isOk());

        // Verify vxp event is generated
        Mockito.verify(this.drcExternalEventProducer, Mockito.times(1)).send(drcExternalEventDtoArgumentCaptor.capture());

        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiInitiatedCounter();
        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiCallInvokedSuccessfullyCounter();

        verifyDrcExternalEventDto("informing_loop_started");
    }


    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRCService request Endpoint invoked  with invalid event type request" +
            "Then verify error response received")
    public void whenDrcApiIsInvokedForInvalidEventTypeThenGetSuccessHttpStatus() throws Exception {

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                        .content(JacksonUtil.getMapper().writeValueAsBytes(buildDrcNotificationRequestDTO())))
                .andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRCService request Endpoint invoked  with INFORMING_LOOP_DECISION request object" +
            "Then verify success received")
    public void whenDrcApiIsInvokedForInformingLoopDecisionThenGetSuccessHttpStatus() throws Exception {
        drcNotificationRequestDTO.setEvent(EventTypes.INFORMING_LOOP_DECISION);
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"P10100\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(JacksonUtil.getMapper().writeValueAsBytes(drcNotificationRequestDTO)))
                .andExpect(status().isOk());

        // Verify vxp event is generated
        Mockito.verify(this.drcExternalEventProducer, Mockito.times(1)).send(drcExternalEventDtoArgumentCaptor.capture());

        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiInitiatedCounter();
        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiCallInvokedSuccessfullyCounter();

        verifyDrcExternalEventDto("informing_loop_decision");
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRCService request Endpoint invoked  with RESULT_VIEWED request object" +
            "Then verify success received")
    public void whenDrcApiIsInvokedForResultsViewedThenGetSuccessHttpStatus() throws Exception {
        drcNotificationRequestDTO.setEvent(EventTypes.RESULT_VIEWED);
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"P10100\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(JacksonUtil.getMapper().writeValueAsBytes(drcNotificationRequestDTO)))
                .andExpect(status().isOk());

        // Verify vxp event is generated
        Mockito.verify(this.drcExternalEventProducer, Mockito.times(1)).send(drcExternalEventDtoArgumentCaptor.capture());

        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiInitiatedCounter();
        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementRealTimeApiCallInvokedSuccessfullyCounter();

        verifyDrcExternalEventDto("result_viewed");
    }

    private void verifyDrcExternalEventDto(String result_viewed) {
        DRCExternalEventDto drcExternalEventDto = drcExternalEventDtoArgumentCaptor.getValue();
        assertNotNull(drcExternalEventDto);
        assertEquals("P10100", drcExternalEventDto.getExternalID());
        assertEquals(result_viewed, drcExternalEventDto.getEventType());
        assertEquals(67701409, drcExternalEventDto.getVibrentID());
        assertEquals("\"{\\\"module_type\\\": \\\"recreational_genetics\\\",\\\"decision\\\": \\\"yes\\\"}\"", drcExternalEventDto.getBody());
        assertEquals(ExternalEventSourceEnum.DRC, drcExternalEventDto.getSource());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRCService request Endpoint invoked  and request object is null" +
            "Then verify Bad request received And Kafka message is not sent")
    public void whenDrcApiIsInvokedAndRequestObjectIsNullGetBadRequest() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        drcNotificationRequestDTO = null;

        // Verify bad request received
        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(JacksonUtil.getMapper().writeValueAsBytes(drcNotificationRequestDTO)))
                .andExpect(status().isBadRequest());
        // Verify kafka message not sent
        Mockito.verify(this.drcExternalEventProducer, Mockito.times(0)).send(any(DRCExternalEventDto.class));
    }

    @DisplayName("When DRCService notification request Endpoint invoked with invalid external id" +
            "Then verify UnProcessable Entity is received and VXP Event is not generated")
    public void whenDrcNotificationApiIsInvokedWithInvalidExternalIdThenVerifyUnProcessableEntity() throws Exception {
        drcNotificationRequestDTO.setParticipantId("P2");
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[],\"link\":{},\"total\":0}");

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(JacksonUtil.getMapper().writeValueAsBytes(drcNotificationRequestDTO)))
                .andExpect(status().isUnprocessableEntity());

        // Verify vxp event is not generated
        Mockito.verify(this.drcExternalEventProducer, Mockito.times(0)).send(any(DRCExternalEventDto.class));
    }

    @Test
    @DisplayName("When DRC request Endpoint invoked  with invalid request object" +
            "Then verify bad request received")
    public void whenDrcApiIsInvokedWithIncorrectRequestBodyFormatReceive() throws Exception {
        drcNotificationRequestDTO.setEventAuthoredTime(OffsetDateTime.now());

        mockMvc.perform(post(POST_EVENT_NOTIFICATION_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .content(buildInvalidEventTypePayload()))
                .andExpect(status().isBadRequest());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked Then verify success received")
    public void whenDrcGetParticipantLookupIsInvokedThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");
        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("vibrentId", "67787412")
        ).andExpect(status().isOk());

        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementParticipantLookupApiInitiatedCounter();
        Mockito.verify(dataSharingMetricsService, Mockito.times(1)).incrementParticipantLookupApiCallInvokedSuccessfullyCounter();
    }


    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked Without link object Then verify success received")
    public void whenDrcGetParticipantLookupIsInvokedWithoutLinkResponseThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":\"v1\",\"EXTERNAL_ID\": \"p1\"}]}");
        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("vibrentId", "67787412")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with Start Date Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedWithStartDateThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");
        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("startDate", "2021-07-01T00:00:00-00:00")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with End Date Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedWithEndDateThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("endDate", "2021-07-12T00:00:00-00:00")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with page Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedWithPageThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("page", "1")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with page size Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedWithPageSizeThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("pageSize", "20")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with both VibrentId and DrcID Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedWithBothVibrentIdAndDrcIdThenVerifySuccess() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("vibrentId", "1,2,3")
                .param("drcId", "p1,p2,p3")
        ).andExpect(status().isOk());
    }

    @WithMockUser(roles = "DRC")
    @Test
    @DisplayName("When DRC GET Participant Lookup Endpoint invoked with both VibrentId and DrcID Then verify Success response is received")
    public void whenDrcGetParticipantLookupIsInvokedThenVerifySuccessResponseWithLinks() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":67701409,\"EXTERNAL_ID\": \"p1\",\"TEST_PARTICIPANT\":true}],\"link\":{\"previousPageQuery\":\"/api/userInfo/search?page=1&pageSize=1\",\"nextPageQuery\":\"/api/userInfo/search?page=3&pageSize=1\"},\"total\":1}");

        MvcResult result = mockMvc.perform(get(GET_PARTICIPANT_LOOKUP_REQUEST_ENDPOINT).contentType(APPLICATION_JSON_UTF8)
                .param("vibrentId", "1,2,3")
                .param("drcId", "p1,p2,p3")
                .param("startDate", "2021-07-12T00:00:00")
                .param("endDate", "2021-07-22T00:00:00")
        ).andExpect(status().isOk()).andReturn();

        ParticipantLookupResponse response = JacksonUtil.getMapper().readValue(result.getResponse().getContentAsByteArray(), ParticipantLookupResponse.class);
        assertNotNull(response);
        assertNotNull(response.getParticipants());

        List<Participant> participants = response.getParticipants();
        assertEquals(1, participants.size());
        Participant participant = participants.get(0);
        assertNotNull(participant);
        assertEquals("67701409", participant.getVibrentId());
        assertEquals("p1", participant.getDrcId());
        assertTrue(participant.getTestParticipant());
        assertEquals(1, response.getTotal());

        ParticipantLookupResponseLink link = response.getLink();
        assertNotNull(response.getLink());
        assertNotNull(link.getPreviousPageQuery());
        assertNotNull(link.getNextPageQuery());
        assertTrue(link.getPreviousPageQuery().contains("/api/v1/drc/participantLookup?vibrentId=1,2,3&drcId=p1,p2,p3&startDate=2021-07-12T00:00:00&endDate=2021-07-22T00:00:00&page=1&pageSize=1"));
        assertTrue(link.getNextPageQuery().contains("/api/v1/drc/participantLookup?vibrentId=1,2,3&drcId=p1,p2,p3&startDate=2021-07-12T00:00:00&endDate=2021-07-22T00:00:00&page=3&pageSize=1"));
    }


    private DrcNotificationRequestDTO buildDrcNotificationRequestDTO() {
        return buildDrcNotificationRequestDTO(EventTypes.INFORMING_LOOP_STARTED);
    }

    private DrcNotificationRequestDTO buildDrcNotificationRequestDTO(EventTypes eventTypes) {
        DrcNotificationRequestDTO drcNotificationRequestDTO = new DrcNotificationRequestDTO();
        drcNotificationRequestDTO.setEvent(eventTypes);
        drcNotificationRequestDTO.setParticipantId("10100");
        drcNotificationRequestDTO.setMessageBody("{\"module_type\": \"recreational_genetics\",\"decision\": \"yes\"}");
        return drcNotificationRequestDTO;
    }


    @SneakyThrows
    private String buildInvalidEventTypePayload() {
        String payload = JacksonUtil.getMapper().writeValueAsString(buildDrcNotificationRequestDTO(EventTypes.INFORMING_LOOP_STARTED));
        return payload.replace(EventTypes.INFORMING_LOOP_STARTED.getValue(), "INVALID_TYPE");
    }
}