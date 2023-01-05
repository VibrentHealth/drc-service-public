package com.vibrent.drc.service.impl;

import com.vibrent.acadia.domain.enumeration.AddressType;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.drc.enumeration.DRCSupplyMessageStatusType;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.GenotekService;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.fulfillment.dto.ProductDTO;
import com.vibrent.fulfillment.dto.TrackingDetailsDTO;
import com.vibrent.fulfillment.dto.TrackingTypeEnum;
import com.vibrent.genotek.vo.OrderInfoDTO;
import com.vibrent.vxp.workflow.*;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FHIRSalivaryConverterUtilityTest {

    private String testWorkflowId = "1D2L3F4G";
    private String testMessageId = "vxpmessage123";
    private String testParticipantId = "P12345676534";
    private String testTrackingId = "PARTTRACK123";
    private Long testTime = 1554399635000L;
    private Long testUserId = 132434L;
    private static final long VIBRENT_ID = 1000L;
    private FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;
    private UserDTO userDTO;
    public static final String BIOBANK_ADDRESS_API_RESPONSE = "{\"city\": \"Rochester\", \"line\": [\"Mayo Clinic Laboratories\", \"3050 Superior Drive NW\"], \"state\": \"MN\", \"postalCode\": \"55901\"}";

    @Mock
    private ApiService apiService;

    @Mock
    private GenotekService genotekService;

    @BeforeEach
    void setUp() {
        fhirSalivaryConverterUtility = new FHIRSalivaryConverterUtility("https://pmi-drc-api-test.appspot.com/rdr/v1", apiService, genotekService);
        initializeUserDTO();
    }

    @Test
    public void testOrderToSupplyRequestFHIRConverterMissingCreateTrackOrderMessage() {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.orderToSupplyRequestFHIRConverter(null, messageHeaderDto, SupplyRequest.SupplyRequestStatus.ACTIVE, "100L"));
    }

    @Test
    public void testOrderToSupplyRequestFHIRConverterMissingHeaderDto() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.orderToSupplyRequestFHIRConverter(createTrackOrderResponseDTO(), null, SupplyRequest.SupplyRequestStatus.ACTIVE, "100L"));
    }

    @Test
    public void testOrderToSupplyDeliveryFHIRConverterMissingCreateTrackOrderMessage() {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.orderToSupplyDeliveryFHIRConverter(null, SupplyDelivery.SupplyDeliveryStatus.INPROGRESS,
                        DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,
                        messageHeaderDto, new IdentifierDto()));
    }

    @Test
    public void testOrderToSupplyDeliveryFHIRConverterMissingHeaderDto() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.orderToSupplyDeliveryFHIRConverter(createTrackDeliveryResponseDto(), SupplyDelivery.SupplyDeliveryStatus.INPROGRESS,
                        DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,
                        null, new IdentifierDto()));
    }

    @Test
    void setParticipantAddress() {
        ParticipantDto participantDto = getParticipantDto();

        //Code under test
        fhirSalivaryConverterUtility.setParticipantAddress(participantDto);

        assertNotNull(participantDto.getAddresses());
        assertEquals(1, participantDto.getAddresses().size());

        AddressDto addressDto = participantDto.getAddresses().get(0);
        assertEquals("Mesa", addressDto.getCity());
        assertEquals("6644 E Baywood Ave", addressDto.getLine1());
        assertEquals("85206", addressDto.getPostalCode());
        assertEquals("AZ", addressDto.getState());
    }


    @Test
    @DisplayName("When null Fulfillment order response received " +
            "then throw BusinessValidationException")
    public void testFulfillmentOrderToSupplyRequestFHIRConverterMissingCreateOrderMessage() {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyRequestFHIRConverter(null, messageHeaderDto, SupplyRequest.SupplyRequestStatus.ACTIVE, "100", getParticipantDto()));
    }

    @Test
    @DisplayName("When header dto is missing " +
            "then throw BusinessValidationException")
    public void testFulfillmentOrderToSupplyRequestFHIRConverterMissingHeaderDto() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyRequestFHIRConverter(createFulfillmentResponseDto(), null, SupplyRequest.SupplyRequestStatus.ACTIVE, "100", getParticipantDto()));
    }

    @Test
    @DisplayName("When Fulfillment order response received with invalid work fow type " +
            "then throw BusinessValidationException")
    void testFulfillmentOrderToSupplyRequestFHIRConverterWithInvalidWorkFlow() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        //EXPECT Business validation on wrong work flow type
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyRequest incorrect workflow name for order type", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyRequestFHIRConverter(createFulfillmentResponseDto(), messageHeaderDto, SupplyRequest.SupplyRequestStatus.ACTIVE, "100", getParticipantDto()));
    }

    @Test
    @DisplayName("When Fulfillment order response received with status SHIPPED and message header " +
            "then process request and provide supply request")
    @SneakyThrows
    void testFulfillmentOrderToSupplyRequestFHIRConverterForShippedStatus() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());

        SupplyRequest supplyRequest = fhirSalivaryConverterUtility.fulfillmentOrderToSupplyRequestFHIRConverter(createFulfillmentResponseDto(), messageHeaderDto, SupplyRequest.SupplyRequestStatus.ACTIVE, "100", getParticipantDto());

        assertNotNull(supplyRequest);
        assertEquals(new BigDecimal(1),supplyRequest.getQuantity().getValue());
        assertEquals(SupplyRequest.SupplyRequestStatus.ACTIVE,supplyRequest.getStatus());
        assertEquals(3,supplyRequest.getExtension().size());
        assertEquals(3,supplyRequest.getContained().size());
        assertEquals(2,supplyRequest.getIdentifier().size());
        assertEquals("100",supplyRequest.getIdentifier().get(0).getValue());
    }

    @Test
    @DisplayName("When null tracking details response received  " +
            "then throw BusinessValidationException")
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterMissingCreateOrderMessage() {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyDelivery cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(),null, getParticipantDto(), SupplyDelivery.SupplyDeliveryStatus.COMPLETED, DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,messageHeaderDto,100L));
    }

    @Test
    @DisplayName("When header dto is missing " +
            "then throw BusinessValidationException")
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterMissingHeaderDto() {
        TrackingDetailsDTO trackingDetailsDto= getTrackingDetailsDto("233","PARTICIPANT_DELIVERED",TrackingTypeEnum.PARTICIPANT_TRACKING,"usps",232324334L,7676759L);
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyDelivery cannot convert missing request", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(), trackingDetailsDto, getParticipantDto(), SupplyDelivery.SupplyDeliveryStatus.COMPLETED, DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,null,100L));
    }

    @Test
    @DisplayName("When Fulfillment order response received with invalid work fow type " +
            "then throw BusinessValidationException")
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterWithInvalidWorkFlow() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackingDetailsDTO trackingDetailsDto= getTrackingDetailsDto("233","PARTICIPANT_DELIVERED",TrackingTypeEnum.PARTICIPANT_TRACKING,"usps",232324334L,7676759L);
        //EXPECT Business validation on wrong work flow type
        Assert.assertThrows("FHIRSalivaryConverterUtility: supplyDelivery incorrect workflow name for order type", BusinessValidationException.class,
                () -> fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(), trackingDetailsDto, getParticipantDto(), SupplyDelivery.SupplyDeliveryStatus.COMPLETED, DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,null,100L));
    }

    @Test
    @DisplayName("When Fulfillment order response received with status PARTICIPANT_DELIVERED and message header " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterForParticipantStatus() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());

        TrackingDetailsDTO trackingDetailsDto= getTrackingDetailsDto("233","PARTICIPANT_DELIVERED",TrackingTypeEnum.PARTICIPANT_TRACKING,"usps",232324334L,7676759L);
        SupplyDelivery supplyDelivery = fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(), trackingDetailsDto, getParticipantDto(), SupplyDelivery.SupplyDeliveryStatus.COMPLETED, DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY,messageHeaderDto,100L);

        assertNotNull(supplyDelivery);
        assertEquals(SupplyDelivery.SupplyDeliveryStatus.COMPLETED,supplyDelivery.getStatus());
        assertEquals(4,supplyDelivery.getExtension().size());
        assertEquals(3,supplyDelivery.getContained().size());
        assertEquals(1,supplyDelivery.getIdentifier().size());
        assertEquals("233",supplyDelivery.getIdentifier().get(0).getValue());
    }

    @Test
    @DisplayName("When Fulfillment order response received with status PARTICIPANT_IN_TRANSIT but no mailing address " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterForParticipantInTransitStatus() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());

        var participant = getParticipantDto();
        participant.setAddresses(null);
        TrackingDetailsDTO trackingDetailsDto= getTrackingDetailsDto("233","PARTICIPANT_IN_TRANSIT",TrackingTypeEnum.PARTICIPANT_TRACKING,"usps",232324334L,7676759L);
        SupplyDelivery supplyDelivery = fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(), trackingDetailsDto, participant, SupplyDelivery.SupplyDeliveryStatus.INPROGRESS, DRCSupplyMessageStatusType.PARTICIPANT_SHIPPED,messageHeaderDto,100L);

        assertNotNull(supplyDelivery);
        assertEquals(SupplyDelivery.SupplyDeliveryStatus.INPROGRESS,supplyDelivery.getStatus());
        assertEquals(4,supplyDelivery.getExtension().size());
        assertEquals(3,supplyDelivery.getContained().size());
        assertEquals(1,supplyDelivery.getIdentifier().size());
        assertEquals("233",supplyDelivery.getIdentifier().get(0).getValue());
    }

    @Test
    @DisplayName("When Fulfillment order response received with status RETURN_IN_TRANSIT and message header " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testFulfillmentOrderToSupplyDeliveryFHIRConverterForReturnStatus() {
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);

        TrackingDetailsDTO trackingDetailsDto= getTrackingDetailsDto("8897","RETURN_IN_TRANSIT",TrackingTypeEnum.RETURN_TRACKING,"usps",232324334L,7676759L);
        SupplyDelivery supplyDelivery = fhirSalivaryConverterUtility.fulfillmentOrderToSupplyDeliveryFHIRConverter(createFulfillmentResponseDto(), trackingDetailsDto, getParticipantDto(), SupplyDelivery.SupplyDeliveryStatus.INPROGRESS, DRCSupplyMessageStatusType.BIOBANK_SHIPPED,messageHeaderDto,100L);

        assertNotNull(supplyDelivery);
        assertEquals(SupplyDelivery.SupplyDeliveryStatus.INPROGRESS,supplyDelivery.getStatus());
        assertEquals(4,supplyDelivery.getExtension().size());
        assertEquals(3,supplyDelivery.getContained().size());
        assertEquals(1,supplyDelivery.getIdentifier().size());
        assertEquals("8897",supplyDelivery.getIdentifier().get(0).getValue());
    }

    private ParticipantDto getParticipantDto() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(VIBRENT_ID);
        participantDto.setAddresses(getParticipantAddress());
        return participantDto;
    }

    private List<AddressDto> getParticipantAddress() {
        List<AddressDto> addressDtoList = new ArrayList<>();
        AddressDto addressDto = new AddressDto();
        addressDto.setCity("Mesa");
        addressDto.setLine1("6644 E Baywood Ave");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        addressDto.setAddressType(TypeOfAddressEnum.HOME_ADDRESS);
        addressDtoList.add(addressDto);
        return addressDtoList;
    }

    private void initializeUserDTO() {
        userDTO = new UserDTO();
        userDTO.setId(VIBRENT_ID);
        userDTO.setTestUser(false);
        userDTO.setUserAddresses(getUserMailingAddress());
    }

    private List<UserAddressDTO> getUserMailingAddress() {
        List<UserAddressDTO> mailingAddress = new ArrayList<>();
        UserAddressDTO addressDto = new UserAddressDTO();
        addressDto.setCity("Mesa");
        addressDto.setStreetOne("6644 E Baywood Ave");
        addressDto.setZip("85206");
        addressDto.setState("AZ");
        addressDto.setType(AddressType.MAILING);
        mailingAddress.add(addressDto);
        return mailingAddress;
    }

    private MessageHeaderDto createMessageHeaderDTO() {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpWorkflowInstanceID(testWorkflowId);
        messageHeaderDto.setVxpMessageID(testMessageId);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        return messageHeaderDto;
    }

    private CreateTrackOrderResponseDto createTrackOrderResponseDTO() {
        //SET request and mock global database object
        CreateTrackOrderResponseDto createTrackOrderResponseDto = new CreateTrackOrderResponseDto();

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();

        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        createTrackOrderResponseDto.setParticipant(participant);

        createTrackOrderResponseDto.setProvider(ProviderEnum.GENOTEK);
        createTrackOrderResponseDto.setDateTime(testTime);
        return createTrackOrderResponseDto;
    }

    private TrackDeliveryResponseDto createTrackDeliveryResponseDto() {
        //SET request and mock global database object
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();
        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        participant.setAddresses(getMailingAddress());
        trackDeliveryResponseDto.setParticipant(participant);

        trackDeliveryResponseDto.setTrackingID(testTrackingId);
        trackDeliveryResponseDto.setDateTime(testTime);
        trackDeliveryResponseDto.setProvider(ProviderEnum.USPS);
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

    private TrackingDetailsDTO getTrackingDetailsDto(String trackingId, String status, TrackingTypeEnum trackingType, String carrierCode, Long deliveredOn, Long shippedOn) {
        TrackingDetailsDTO trackingDetailsDTO = new TrackingDetailsDTO();
        trackingDetailsDTO.setTrackingId(trackingId);
        trackingDetailsDTO.setStatus(status);
        trackingDetailsDTO.setTrackingType(trackingType);
        trackingDetailsDTO.setCarrierCode(carrierCode);
        trackingDetailsDTO.setDeliveredOn(deliveredOn);
        trackingDetailsDTO.setShippedOn(shippedOn);
        return trackingDetailsDTO;
    }

    private FulfillmentResponseDto createFulfillmentResponseDto() {
        FulfillmentResponseDto fulfillmentResponseDto = new FulfillmentResponseDto();

        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);
        fulfillmentResponseDto.setProgramID(123654L);
        fulfillmentResponseDto.setVibrentID(136524L);

        OrderDto orderDto = new OrderDto();
        orderDto.setFulfillmentOrderID(100L);
        orderDto.setQuantity(1L);
        fulfillmentResponseDto.setOrder(orderDto);

        Map<String,String> attributeMap = Map.of(
                "FULFILLMENT_ID","100",
                "BARCODE_1D","888999"
        );
        fulfillmentResponseDto.setAttributes(attributeMap);
        return fulfillmentResponseDto;
    }

    private OrderInfoDTO buildOrderInfoDTO() {
        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        orderInfoDTO.setOrderType("Salivary Order");
        orderInfoDTO.setItemCode("4081");
        orderInfoDTO.setItemName("OGD-500.015");
        return orderInfoDTO;
    }

}