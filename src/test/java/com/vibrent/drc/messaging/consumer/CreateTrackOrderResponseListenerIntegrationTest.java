package com.vibrent.drc.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.integration.IntegrationTest;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.DRCSupplyStatusService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
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

import java.util.*;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
public class CreateTrackOrderResponseListenerIntegrationTest extends IntegrationTest {

    private String testParticipantId = "P12345676534";
    private Long testUserId = 132434L;
    private Long testTime = 1554399635000L;
    private String testOrderId = "123456";

    @Autowired
    private DRCSalivaryOrderService drcSalivaryOrderService;

    @Autowired
    private CreateTrackOrderResponseListener createTrackOrderResponseListener;

    @MockBean
    private DRCSupplyStatusService drcSupplyStatusService;

    @Mock
    private DRCConfigService drcConfigService;

    @MockBean
    private ApiService apiService;

    @MockBean
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Captor
    private ArgumentCaptor<String> fhirMessage;

    @Test
    @DisplayName("When Create track order response event received then verify event get processed. ")
    public void testCreateTrackOrderResponseListener() throws Exception {
        when(this.apiService.getDeviceDetails()).thenReturn("{\"SKU\": 4081,\"name\": \"OGD-500.015\",\"type\": \"manufacturer-name\",\"display\": \"Oragene.Dx self-collection kit\"}");

        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.CREATED);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdIdentifier()));

        createTrackOrderResponseListener.listen(buildPayload(createTrackOrderResponseDto), buildMessageHeaders());
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}],\"address\":[{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"CREATED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}],\"status\":\"active\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1},\"authoredOn\":\"2019-04-04T17:40:35+00:00\",\"requester\":{\"reference\":\"#patient-1\"},\"supplier\":[{\"reference\":\"#supplier-1\"}],\"deliverFrom\":{\"reference\":\"#supplier-1\"},\"deliverTo\":{\"reference\":\"#patient-1\"}}",
                drcPayload);
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

        createTrackOrderResponseDto.setProvider(ProviderEnum.GENOTEK);
        createTrackOrderResponseDto.setDateTime(testTime);
        return createTrackOrderResponseDto;
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

}