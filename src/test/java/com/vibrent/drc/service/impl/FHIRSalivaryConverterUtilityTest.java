package com.vibrent.drc.service.impl;

import com.vibrent.acadia.domain.enumeration.AddressType;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.drc.enumeration.DRCSupplyMessageStatusType;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.GenotekService;
import com.vibrent.vxp.workflow.*;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        when(this.apiService.getUserDTO(VIBRENT_ID)).thenReturn(userDTO);

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

    private ParticipantDto getParticipantDto() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(VIBRENT_ID);
        return participantDto;
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
}