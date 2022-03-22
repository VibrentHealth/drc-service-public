package com.vibrent.drc.integration.messaging;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.drc.messaging.consumer.TrackDeliveryResponseListener;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackDeliveryResponseListenerTest {

    public static final String TOPIC_NAME = "topicName";
    private final String testParticipantId = "P12345676534";
    private final Long testUserId = 132434L;
    private final Long testTime = 1554399635000L;
    private final String TRACKING_ID = "tracking_1";
    private final String orderIdString = "782930";

    private OrderTrackingDetails orderTrackingDetails;

    private TrackDeliveryResponseListener trackDeliveryResponseListener;

    @Mock
    private DRCSalivaryOrderService drcSalivaryOrderService;

    @Mock
    private FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Mock
    private OrderTrackingDetailsService orderTrackingDetailsService;

    @BeforeEach
    void setUp() {
        trackDeliveryResponseListener = new TrackDeliveryResponseListener(drcSalivaryOrderService, fhirSalivaryConverterUtility, TOPIC_NAME, externalApiRequestLogsService, orderTrackingDetailsService);
    }

    @Test
    @DisplayName("When track deliver response event received" +
            "then verify event get processed. ")
    void testVxpListener() throws IOException {
        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto()), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received" +
            "then verify external event is sent.")
    void testWhenTrackDeliverResponseEventReceivedThenVerifyExternalEventIsSent() throws IOException {
        MessageHeaders messageHeaders = buildMessageHeaders();
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), messageHeaders);

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.AFTER_SHIP, actual.getService());
        assertEquals(RequestMethod.GET , actual.getHttpMethod());
        assertEquals("Kafka topic: " + TOPIC_NAME, actual.getRequestUrl());
        assertNotEquals(PrettyPrintUtil.prettyPrint(messageHeaders), actual.getRequestHeaders());
        assertTrue(StringUtils.isBlank(actual.getRequestBody()));
        assertEquals(PrettyPrintUtil.prettyPrint(trackDeliveryResponseDto), actual.getResponseBody());
        assertEquals(200 , actual.getResponseCode());
        assertNotEquals(0L , actual.getRequestTimestamp());
        assertNotEquals(0L , actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(testParticipantId,  actual.getExternalId());
        assertEquals(ExternalEventType.AFTER_SHIP_TRACKING_RESPONSE_RECEIVED , actual.getEventType());
        assertEquals("DRC Service received message from VXP tracking service", actual.getDescription());
        assertEquals(ExternalEventSource.AFTER_SHIP_SERVICE , actual.getEventSource());
    }

    @Test
    @DisplayName("When track deliver response event received with invalid payload +\n" +
            "then verify event is not processed. ")
    void testVxpListenerWhenInvalidPayload() throws IOException {
        trackDeliveryResponseListener.listen(JacksonUtil.getMapper().writeValueAsBytes("InValidString"), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received with ERROR +\n" +
            "then verify event is not processed. ")
    void testVxpListenerWhenErrorPayload() throws IOException {
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseDto.setErrors(buildErrors());

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received with IN_TRANSIT and Last Message status is DELIVERED " +
            "then verify event is not processed. ")
    void testTrackDeliveryResponseWhenInTransitPayloadReceivedAndLastMessageStatusIsDelivered() throws IOException {
        initializeOrderTrackingDetails();
        when(orderTrackingDetailsService.getOrderDetails(TRACKING_ID)).thenReturn(orderTrackingDetails);

        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received with DELIVERED and Last Message status is IN_TRANSIT " +
            "then verify event is processed. ")
    void testTrackDeliveryResponseWhenDeliveredPayloadReceivedAndLastMessageStatusIsDelivered() throws IOException {
        initializeOrderTrackingDetails();
        when(orderTrackingDetailsService.getOrderDetails(TRACKING_ID)).thenReturn(orderTrackingDetails);

        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received with ERROR then verify event is processed. ")
    void testTrackDeliveryResponseWhenDeliveredPayloadReceivedWithError() throws IOException {
        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.ERROR);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    @Test
    @DisplayName("When track deliver response event received with DELIVERED and Last Message status is DELIVERED " +
            "then verify event is not processed. ")
    void testTrackDeliveryResponseWhenDeliveredPayloadReceivedAndLastMessageStatusIsInTransit() throws IOException {
        initializeOrderTrackingDetails();
        orderTrackingDetails.setLastMessageStatus(StatusEnum.IN_TRANSIT.toValue());
        when(orderTrackingDetailsService.getOrderDetails(TRACKING_ID)).thenReturn(orderTrackingDetails);

        TrackDeliveryResponseDto trackDeliveryResponseDto = trackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendTrackDeliveryResponse(any(TrackDeliveryResponseDto.class), any(MessageHeaderDto.class));
    }

    private List<ErrorPayloadDto> buildErrors() {
        List<ErrorPayloadDto> errorPayloadDtos = new ArrayList<>();
        ErrorPayloadDto errorPayloadDto = new ErrorPayloadDto();
        errorPayloadDto.setCode(ErrorCodeEnum.INVALID_ADDRESS);
        errorPayloadDto.setMessage("Invalid Message");
        errorPayloadDtos.add(errorPayloadDto);
        return errorPayloadDtos;
    }

    private byte[] buildPayload(TrackDeliveryResponseDto trackDeliveryResponseDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(trackDeliveryResponseDto);
    }

    private TrackDeliveryResponseDto trackDeliveryResponseDto() {
        //SET request and mock global database object
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setTrackingID(TRACKING_ID);

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();
        participant.setAddresses(getMailingAddress());

        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        trackDeliveryResponseDto.setParticipant(participant);
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);

        trackDeliveryResponseDto.setProvider(ProviderEnum.USPS);
        trackDeliveryResponseDto.setDateTime(testTime);
        return trackDeliveryResponseDto;
    }

    MessageHeaders buildMessageHeaders() {
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

    private void initializeOrderTrackingDetails() {
        orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setOrderId(Long.valueOf(orderIdString));
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID);
        orderTrackingDetails.setLastMessageStatus(StatusEnum.DELIVERED.toValue());
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
}