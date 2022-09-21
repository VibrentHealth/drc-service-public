package com.vibrent.drc.integration.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.integration.IntegrationTest;
import com.vibrent.drc.messaging.consumer.TrackDeliveryResponseListener;
import com.vibrent.drc.service.*;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.genotek.vo.OrderInfoDTO;
import com.vibrent.vxp.workflow.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static com.vibrent.drc.domain.OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
public class TrackDeliveryResponseListenerIntegrationTest extends IntegrationTest {

    public static final String BIOBANK_ADDRESS_API_RESPONSE = "{\"city\": \"Rochester\", \"line\": [\"Mayo Clinic Laboratories\", \"3050 Superior Drive NW\"], \"state\": \"MN\", \"postalCode\": \"55901\"}";

    private String testParticipantId = "P12345676534";
    private Long testUserId = 132434L;
    private Long testTime = 1554399635000L;
    private String testOrderId = "123456";
    private String testTrackingId = "PARTTRACK123";

    @Autowired
    private DRCSalivaryOrderService drcSalivaryOrderService;

    @Autowired
    private TrackDeliveryResponseListener trackDeliveryResponseListener;

    @MockBean
    private DRCSupplyStatusService drcSupplyStatusService;

    @Mock
    private DRCConfigService drcConfigService;

    @MockBean
    private ApiService apiService;

    @MockBean
    private GenotekService genotekService;

    @MockBean
    private OrderTrackingDetailsService orderTrackingDetailsService;

    @MockBean
    ExternalApiRequestLogsService externalApiRequestLogsService;

    @Captor
    private ArgumentCaptor<String> fhirMessage;

    @Test
    @DisplayName("When Track Delivery response event received for participant shipped then verify event get processed and send to DRC")
    public void testListenWhenSupplyDeliveryParticipantShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());

        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"status\":\"in-progress\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                drcPayload);
        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(testTrackingId);
        Assertions.assertNotNull(orderDetails);
        Assertions.assertEquals("IN_TRANSIT",orderDetails.getLastMessageStatus());
    }

    @Test
    @DisplayName("When Track Delivery response event for participant delivery received then verify event get processed and send to DRC")
    public void testListenWhenSupplyDeliveryParticipantDelivery() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());

        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
              drcPayload);

        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(testTrackingId);
        Assertions.assertNotNull(orderDetails);
        Assertions.assertEquals("DELIVERED",orderDetails.getLastMessageStatus());

    }

    @Test
    @DisplayName("When Track Delivery response event for biobank shipped received then verify event get processed and send to DRC")
    public void testListenWhenSupplyDeliveryBioBankShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());

        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}}],\"status\":\"in-progress\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
             drcPayload);

        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(testTrackingId);
        Assertions.assertNotNull(orderDetails);
        Assertions.assertEquals("IN_TRANSIT", orderDetails.getLastMessageStatus());
    }

    @Test
    @DisplayName("When Track Delivery response event for biobank delivery received then verify event get processed and send to DRC")
    public void testListenWhenSupplyDeliveryBioBankDelivery() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());

        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        trackDeliveryResponseListener.listen(buildPayload(trackDeliveryResponseDto), buildMessageHeaders());
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());

        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                drcPayload);

        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(testTrackingId);
        Assertions.assertNotNull(orderDetails);
        Assertions.assertEquals("DELIVERED", orderDetails.getLastMessageStatus());
    }

    private OrderTrackingDetails getBiobankTrackingOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testTrackingId);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderId));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }

    private byte[] buildPayload(TrackDeliveryResponseDto trackDeliveryResponseDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(trackDeliveryResponseDto);
    }

    private TrackDeliveryResponseDto createTrackDeliveryResponseDto() {
        //SET request and mock global database object
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();
        participant.setAddresses(getMailingAddress());

        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        trackDeliveryResponseDto.setParticipant(participant);

        trackDeliveryResponseDto.setProvider(ProviderEnum.USPS);
        trackDeliveryResponseDto.setDateTime(testTime);
        trackDeliveryResponseDto.setTrackingID(testTrackingId);
        return trackDeliveryResponseDto;
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

    private IdentifierDto getOrderIdIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testOrderId);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.ORDER_ID);
        return identifier;
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

    private OrderTrackingDetails getParticipantTrackingOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testTrackingId);
        orderTrackingDetails.setIdentifierType(PARTICIPANT_TRACKING_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderId));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }
    private HttpResponseWrapper getHttpResponseWrapper(){
        return new HttpResponseWrapper(201,"response");
    }

    private OrderInfoDTO buildOrderInfoDTO() {
        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        orderInfoDTO.setOrderType("Salivary Order");
        orderInfoDTO.setItemCode("4081");
        orderInfoDTO.setItemName("OGD-500.015");
        return orderInfoDTO;
    }
}