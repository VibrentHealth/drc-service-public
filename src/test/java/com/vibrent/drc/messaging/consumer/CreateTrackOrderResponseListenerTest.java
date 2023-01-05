package com.vibrent.drc.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.service.OrderTrackingDetailsService;
import com.vibrent.drc.service.impl.FHIRSalivaryConverterUtility;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.PrettyPrintUtil;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateTrackOrderResponseListenerTest {

    public static final String TOPIC_NAME = "topicName";
    private String testParticipantId = "P12345676534";
    private Long testUserId = 132434L;
    private Long testTime = 1554399635000L;
    private String orderIdString = "782930";

    private OrderTrackingDetails orderTrackingDetails;

    @Mock
    private DRCSalivaryOrderService drcSalivaryOrderService;

    @Mock
    private FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;

    private CreateTrackOrderResponseListener createTrackOrderResponseListener;

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Mock
    private OrderTrackingDetailsService orderTrackingDetailsService;

    @BeforeEach
    void setUp() {
        createTrackOrderResponseListener = new CreateTrackOrderResponseListener(drcSalivaryOrderService, fhirSalivaryConverterUtility, TOPIC_NAME, externalApiRequestLogsService, orderTrackingDetailsService);
    }

    @Test
    @DisplayName("When Create track order response event received then verify event get processed. ")
    void testCreateTrackOrderResponseListener() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track order response event received with Empty Identifiers then verify event is not sent to DRC")
    void testCreateTrackOrderResponseListenerWhenWithEmptyIdentifiers() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.ERROR);
        createTrackOrderResponseDto.setIdentifiers(new ArrayList<>());
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());

        verify(drcSalivaryOrderService, times(0)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When Error track order response event received then verify event is sent to DRC")
    void testCreateTrackOrderResponseListenerWhenErrorOrderReceived() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.ERROR);
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());

        verify(drcSalivaryOrderService, times(1)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When Shipped track order response event received and Last message status is Shipped then verify event will sent to DRC again")
    void testCreateTrackOrderResponseListenerWhenShippedOrderReceived() throws Exception {
        initializeOrderTrackingDetails();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.SHIPPED);
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());

        verify(drcSalivaryOrderService, times(1)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When error while converting Create track order response then event is not processed ")
    void testListenWhenErrorWhileConvertingCreateTrackOrderResponse() throws Exception {
        createTrackOrderResponseListener.listen("[]".getBytes(StandardCharsets.UTF_8), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When Create track order response received with Errors then event is not processed ")
    void testListenWhenCreateTrackOrderResponseReceivedWithErrors() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setErrors(getGenotekTechnicalErrors());
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When Create track order response event with created status received then verify external event sent. ")
    void testCreateTrackOrderResponseReceivedThenVerifyExternalEventSent() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        MessageHeaders messageHeaders = buildMessageHeaders();
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), messageHeaders);
        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        verify(externalApiRequestLogsService, times(1)).send(captor.capture());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.VXP_GENOTEK, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals("Kafka topic: " + TOPIC_NAME, actual.getRequestUrl());
        assertNotEquals(PrettyPrintUtil.prettyPrint(messageHeaders), actual.getRequestHeaders());
        assertTrue(StringUtils.isBlank(actual.getRequestBody()));
        assertEquals(PrettyPrintUtil.prettyPrint(createTrackOrderResponseDto), actual.getResponseBody());
        assertEquals(200 , actual.getResponseCode());
        assertNotEquals(0L , actual.getRequestTimestamp());
        assertNotEquals(0L , actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(testParticipantId,  actual.getExternalId());
        assertEquals(ExternalEventType.GENOTEK_CREATE_ORDER_RESPONSE_RECEIVED , actual.getEventType());
        assertEquals("DRC Service received message from VXP order service", actual.getDescription());
        assertEquals(ExternalEventSource.GENOTEK_SERVICE , actual.getEventSource());
    }

    @Test
    @DisplayName("When Create track order response event with shipped status received then verify external event sent. ")
    void testCreateTrackOrderShippedResponseReceivedThenVerifyExternalEventSent() throws Exception {
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.SHIPPED);

        MessageHeaders messageHeaders = buildMessageHeaders();
        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), messageHeaders);
        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);

        verify(externalApiRequestLogsService, times(1)).send(captor.capture());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendCreateTrackOrderResponse(any(CreateTrackOrderResponseDto.class), any(MessageHeaderDto.class));

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.VXP_GENOTEK, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals("Kafka topic: " + TOPIC_NAME, actual.getRequestUrl());
        assertNotEquals(PrettyPrintUtil.prettyPrint(messageHeaders), actual.getRequestHeaders());
        assertTrue(StringUtils.isBlank(actual.getRequestBody()));
        assertEquals(PrettyPrintUtil.prettyPrint(createTrackOrderResponseDto), actual.getResponseBody());
        assertEquals(200 , actual.getResponseCode());
        assertNotEquals(0L , actual.getRequestTimestamp());
        assertNotEquals(0L , actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(testParticipantId,  actual.getExternalId());
        assertEquals(ExternalEventType.GENOTEK_STATUS_UPDATE_RESPONSE_RECEIVED, actual.getEventType());
        assertEquals("DRC Service received message from VXP order service", actual.getDescription());
        assertEquals(ExternalEventSource.GENOTEK_SERVICE , actual.getEventSource());
    }


    private List<ErrorPayloadDto> getGenotekTechnicalErrors() {
        List<ErrorPayloadDto> errors = new ArrayList<>();
        ErrorPayloadDto errorPayloadDto = new ErrorPayloadDto();
        errorPayloadDto.setCode(ErrorCodeEnum.GENOTEK_TECHNICAL_ERROR);
        errorPayloadDto.setMessage("Technical Error");
        errors.add(errorPayloadDto);
        return errors;
    }

    private byte[] buildPayload(CreateTrackOrderResponseDto createTrackOrderResponseDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(createTrackOrderResponseDto);
    }

    private CreateTrackOrderResponseDto createTrackOrderResponseDTO() {
        //SET request and mock global database object
        CreateTrackOrderResponseDto createTrackOrderResponseDto = new CreateTrackOrderResponseDto();

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();
        participant.setAddresses(getMailingAddress());

        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        createTrackOrderResponseDto.setParticipant(participant);

        createTrackOrderResponseDto.setStatus(StatusEnum.CREATED);
        createTrackOrderResponseDto.setOperation(OperationEnum.CREATE_TRACK_ORDER);
        createTrackOrderResponseDto.setProvider(ProviderEnum.GENOTEK);
        createTrackOrderResponseDto.setDateTime(testTime);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdentifier()));
        return createTrackOrderResponseDto;
    }

    private IdentifierDto getOrderIdentifier() {
        IdentifierDto identifierDto = new IdentifierDto();
        identifierDto.setProvider(ProviderEnum.GENOTEK);
        identifierDto.setType(IdentifierTypeEnum.ORDER_ID);
        identifierDto.setId(orderIdString);
        return identifierDto;
    }

    private List<AddressDto> getMailingAddress() {
        List<AddressDto> mailingAddress = new ArrayList<>();
        AddressDto addressDto = new AddressDto();
        addressDto.setAddressType(TypeOfAddressEnum.MAILING_ADDRESS);
        addressDto.setCity("Mesa");
        addressDto.setCountry("US");
        addressDto.setLine1("6644 E Baywood Ave");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        mailingAddress.add(addressDto);
        return mailingAddress;
    }

    private void initializeOrderTrackingDetails() {
        orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setOrderId(Long.valueOf(orderIdString));
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.ORDER_ID);
        orderTrackingDetails.setLastMessageStatus(StatusEnum.SHIPPED.toValue());
    }

    private MessageHeaders buildMessageHeaders(){
        Map<String, Object> headers = new HashMap<>();
        MessageHeaders messageHeaders;
        headers.put(KAFKA_HEADER_MESSAGE_SPEC, MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE.toValue());
        headers.put(KAFKA_HEADER_REPLY_TO_ID, "inReplyTo");
        headers.put(KAFKA_HEADER_MESSAGE_ID, "messageId");
        headers.put(KAFKA_HEADER_MESSAGE_SPEC_VERSION, "version");
        headers.put(KAFKA_HEADER_MESSAGE_TIMESTAMP, 12345678L);
        headers.put(KAFKA_HEADER_PATTERN, IntegrationPatternEnum.WORKFLOW.toValue());
        headers.put(KAFKA_HEADER_PROGRAM_ID, 2L);
        headers.put(KAFKA_HEADER_TENANT_ID, 1L);
        headers.put(KAFKA_HEADER_TRIGGER, ContextTypeEnum.EVENT.toValue());
        headers.put(KAFKA_HEADER_WORKFLOW_INSTANCE_ID, "instanceID");
        headers.put(KAFKA_HEADER_VERSION, "2.1.4");
        headers.put(KAFKA_HEADER_WORKFLOWNAME, WorkflowNameEnum.SALIVARY_KIT_ORDER.toValue());
        headers.put(KAFKA_HEADER_ORIGINATOR, RequestOriginatorEnum.VXPMS.toValue());

        messageHeaders = new MessageHeaders(headers);
        return messageHeaders;
    }

}