package com.vibrent.drc.service.impl;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.google.common.base.Preconditions;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.drc.constants.SupplyConstants;
import com.vibrent.drc.enumeration.DRCSupplyMessageStatusType;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.GenotekService;
import com.vibrent.fulfillment.dto.TrackingDetailsDTO;
import com.vibrent.genotek.vo.OrderInfoDTO;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.vibrent.drc.constants.SupplyConstants.*;
import static com.vibrent.drc.enumeration.DRCSupplyMessageStatusType.*;

/**
 * NOTE: Utility class for converting Supply objects to SupplyRequestOrder or SupplyDeliveryOrder FHIR
 * Objects.
 */
@Slf4j
@Service
public class FHIRSalivaryConverterUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(FHIRSalivaryConverterUtility.class);
    private static final String STATE_PREFIX = "PIIState_";

    private final String fhirBaseUrl;
    private final ApiService apiService;
    private final GenotekService genotekService;

    public FHIRSalivaryConverterUtility(@Value("${fhir.url.resourceurl:http://joinallofus.org/fhir/}") String fhirBaseUrl,
                                        ApiService apiService, GenotekService genotekService) {
        this.fhirBaseUrl = fhirBaseUrl;
        this.apiService = apiService;
        this.genotekService = genotekService;
    }

    public SupplyRequest orderToSupplyRequestFHIRConverter(CreateTrackOrderResponseDto createTrackOrderResponseDto, MessageHeaderDto messageHeaderDto, SupplyRequest.SupplyRequestStatus status, String orderId) {
        if (createTrackOrderResponseDto == null || messageHeaderDto == null) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request");
        }
        if (messageHeaderDto.getVxpWorkflowName() == null || !messageHeaderDto.getVxpWorkflowName().toValue().equals(SALIVARY_KIT_ORDER)) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyRequest incorrect workflow name for order type");
        }

        //Get orderInfo from genotek
        var orderInfoValue = getOrderInfo(Long.valueOf(orderId));

        //Create SupplyRequestOrder model
        //Every Resource requires text narrative required for conversion
        SupplyRequest supplyRequest = new SupplyRequest();
        supplyRequest.setText(createDefaultNarrative());

        //create and set status
        supplyRequest.setStatus(status);

        //creating contained resources
        supplyRequest.addContained(createOrganization(ORGANIZATION_NAME));
        supplyRequest.addContained(createDevice(orderInfoValue));
        supplyRequest.addContained(createPatient(createTrackOrderResponseDto.getParticipant()));

        // Add Identifier for supplyRequest
        List<IdentifierDto> identifiers = createTrackOrderResponseDto.getIdentifiers() == null ? new ArrayList<>() : createTrackOrderResponseDto.getIdentifiers();
        for (IdentifierDto identifier : identifiers) {
            if (IdentifierTypeEnum.ORDER_ID.equals(identifier.getType()) || IdentifierTypeEnum.FULFILLMENT_ID.equals(identifier.getType())) {
                supplyRequest.addIdentifier(createAndSetIdentifier(identifier.getId(), getIdentifierTypeValue(identifier.getType())));
            } else if (!IdentifierTypeEnum.TRACKING_TO_BIOBANK.equals(identifier.getType()) && !IdentifierTypeEnum.TRACKING_TO_PARTICIPANT.equals(identifier.getType())) {
                supplyRequest.addExtension(createExtension(identifier.getId() == null ? null : new StringType(identifier.getId()), getIdentifierTypeValue(identifier.getType())));
            }
        }
        //add fulfillmentStatus and Order Type
        supplyRequest.addExtension(createExtension(createTrackOrderResponseDto.getStatus() == null ? null : new StringType(createTrackOrderResponseDto.getStatus().name()), FULFILLMENT_STATUS));
        supplyRequest.addExtension(createExtension(new StringType(orderInfoValue.getOrderType()), ORDER_TYPE));

        // Add References
        addSupplyRequestReferences(supplyRequest, createTrackOrderResponseDto);

        return supplyRequest;
    }

    public SupplyRequest fulfillmentOrderToSupplyRequestFHIRConverter(FulfillmentResponseDto fulfillmentResponseDto, MessageHeaderDto messageHeaderDto, SupplyRequest.SupplyRequestStatus status, String orderId, ParticipantDto participantDto) {
        if (fulfillmentResponseDto == null || messageHeaderDto == null) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyRequest cannot convert missing request");
        }
        if (messageHeaderDto.getVxpWorkflowName() == null || !WorkflowNameEnum.FULFILLMENT_KIT_ORDER.equals(messageHeaderDto.getVxpWorkflowName())) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyRequest incorrect workflow name for order type");
        }

        //Get orderInfo from genotek
        var orderInfoValue = getOrderInfo(Long.valueOf(orderId));

        //Create SupplyRequestOrder model
        //Every Resource requires text narrative required for conversion
        SupplyRequest supplyRequest = new SupplyRequest();
        supplyRequest.setText(createDefaultNarrative());

        //create and set status
        supplyRequest.setStatus(status);

        //creating contained resources
        supplyRequest.addContained(createOrganization(ORGANIZATION_NAME));
        supplyRequest.addContained(createDevice(orderInfoValue));
        if (participantDto != null) {
            supplyRequest.addContained(createPatient(participantDto));
        }

        //add fulfillmentStatus and Order Type
        supplyRequest.addExtension(createExtension(new StringType(fulfillmentResponseDto.getStatus().name()), FULFILLMENT_STATUS));
        supplyRequest.addExtension(createExtension(new StringType(orderInfoValue.getOrderType()), ORDER_TYPE));

        // Add Identifier for supplyRequest
        setIdentifierAndExtensionToSupplyRequest(fulfillmentResponseDto, supplyRequest, orderId);

        // Add References
        addSupplyRequestReferences(supplyRequest, fulfillmentResponseDto);

        return supplyRequest;
    }

    public SupplyDelivery fulfillmentOrderToSupplyDeliveryFHIRConverter(FulfillmentResponseDto fulfillmentResponseDto, TrackingDetailsDTO trackingDetailsDTO, ParticipantDto participantDto, SupplyDelivery.SupplyDeliveryStatus status, DRCSupplyMessageStatusType statusType,
                                                                        MessageHeaderDto messageHeaderDto, Long orderId) {
        if (trackingDetailsDTO == null || messageHeaderDto == null) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyDelivery cannot convert missing request");
        }

        if (messageHeaderDto.getVxpWorkflowName() == null || !WorkflowNameEnum.FULFILLMENT_KIT_ORDER.equals(messageHeaderDto.getVxpWorkflowName())) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyDelivery incorrect workflow name for order type");
        }

        //Get orderInfo from genotek
        var orderInfoValue = getOrderInfo(orderId);

        SupplyDelivery supplyDelivery = new SupplyDelivery();
        supplyDelivery.setText(createDefaultNarrative());

        //create and set status
        supplyDelivery.setStatus(status);

        // creating contained resources
        supplyDelivery.addContained(createOrganization(trackingDetailsDTO.getCarrierCode()!= null? trackingDetailsDTO.getCarrierCode() : ORGANIZATION_NAME));
        supplyDelivery.addContained(createDevice(orderInfoValue));

        //Choose location based on status
        supplyDelivery.addContained(createLocation(statusType, participantDto));

        //Add Identifiers for supplyDelivery
        supplyDelivery.addIdentifier(createAndSetIdentifier(trackingDetailsDTO.getTrackingId(), SupplyConstants.TRACKING_ID));

        //Add supplyDelivery extensions
        if (trackingDetailsDTO.getStatus() != null) {
            supplyDelivery.addExtension(createExtension(new StringType(trackingDetailsDTO.getStatus()), SupplyConstants.TRACKING_STATUS_URL));
        }
        Long expectedDeliveryDate = trackingDetailsDTO.getDeliveredOn();
        supplyDelivery.addExtension(createExtension(expectedDeliveryDate == null ? null
                : new DateTimeType(new Date(expectedDeliveryDate), TemporalPrecisionEnum.SECOND), SupplyConstants.EXPECTED_DELIVERY_DATE_URL));
        supplyDelivery.addExtension(createExtension(new StringType(orderInfoValue.getOrderType()), SupplyConstants.ORDER_TYPE));
        supplyDelivery.addExtension(createExtension(new StringType(trackingDetailsDTO.getCarrierCode()), SupplyConstants.CARRIER_URL));


        // Add supply delivery references
        addSupplyDeliveryReferences(supplyDelivery, trackingDetailsDTO, orderId, participantDto);

        if (!statusType.isEqualOrBefore(PARTICIPANT_DELIVERY)) {
            supplyDelivery.addPartOf(new Reference().setIdentifier(createAndSetIdentifier(trackingDetailsDTO.getTrackingId(), SupplyConstants.TRACKING_ID)));
        }

        // create and set suppliedItem
        // Sending it as DEFAULT_QUANTITY. Items never send from TrackDeliveryResponse. So it should be always DEFAULT_QUANTITY
        supplyDelivery.setSuppliedItem(createSupplyDeliveryComponent(DEFAULT_QUANTITY));

        // set occurrence date time
        supplyDelivery.setOccurrence(getStatusTime(fulfillmentResponseDto) <= 0 ? null
                : new DateTimeType(new Date(getStatusTime(fulfillmentResponseDto)), TemporalPrecisionEnum.SECOND));

        return supplyDelivery;
    }

    public SupplyDelivery orderToSupplyDeliveryFHIRConverter(TrackDeliveryResponseDto trackDeliveryResponseDto, SupplyDelivery.SupplyDeliveryStatus status, DRCSupplyMessageStatusType statusType,
                                                             MessageHeaderDto messageHeaderDto, IdentifierDto orderIdentifierDto) {
        if (trackDeliveryResponseDto == null || messageHeaderDto == null) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyDelivery cannot convert missing request");
        }

        if (messageHeaderDto.getVxpWorkflowName() == null || !messageHeaderDto.getVxpWorkflowName().toValue().equals(SALIVARY_KIT_ORDER)) {
            throw new BusinessValidationException("FHIRSalivaryConverterUtility: supplyDelivery incorrect workflow name for order type");
        }

        //Get orderInfo from genotek
        var orderInfoValue = getOrderInfo(Long.valueOf(orderIdentifierDto.getId()));

        SupplyDelivery supplyDelivery = new SupplyDelivery();
        supplyDelivery.setText(createDefaultNarrative());

        //create and set status
        supplyDelivery.setStatus(status);

        // creating contained resources
        supplyDelivery.addContained(createOrganization(ORGANIZATION_NAME));
        supplyDelivery.addContained(createDevice(orderInfoValue));

        //Choose location based on status
        supplyDelivery.addContained(createLocation(trackDeliveryResponseDto, statusType));

        //Add Identifiers for supplyDelivery
        supplyDelivery.addIdentifier(createAndSetIdentifier(trackDeliveryResponseDto.getTrackingID(), SupplyConstants.TRACKING_ID));

        //Add supplyDelivery extensions
        Long expectedDeliveryDate = getDateByType(DateTypeEnum.EXPECTED_DELIVERY_DATE, trackDeliveryResponseDto);
        supplyDelivery.addExtension(createExtension(new StringType(trackDeliveryResponseDto.getStatus().name()), SupplyConstants.TRACKING_STATUS_URL));
        supplyDelivery.addExtension(createExtension(expectedDeliveryDate == null ? null
                : new DateTimeType(new Date(expectedDeliveryDate), TemporalPrecisionEnum.SECOND), SupplyConstants.EXPECTED_DELIVERY_DATE_URL));
        supplyDelivery.addExtension(createExtension(new StringType(orderInfoValue.getOrderType()), SupplyConstants.ORDER_TYPE));
        supplyDelivery.addExtension(createExtension(new StringType(trackDeliveryResponseDto.getProvider().name()), SupplyConstants.CARRIER_URL));


        // Add supply delivery references
        addSupplyDeliveryReferences(supplyDelivery, trackDeliveryResponseDto, orderIdentifierDto);

        if (!statusType.isEqualOrBefore(PARTICIPANT_DELIVERY)) {
            supplyDelivery.addPartOf(new Reference().setIdentifier(createAndSetIdentifier(trackDeliveryResponseDto.getTrackingID(), SupplyConstants.TRACKING_ID)));
        }

        // create and set suppliedItem
        // Sending it as DEFAULT_QUANTITY. Items never send from TrackDeliveryResponse. So it should be always DEFAULT_QUANTITY
        supplyDelivery.setSuppliedItem(createSupplyDeliveryComponent(DEFAULT_QUANTITY));


        // set occurrence date time
        supplyDelivery.setOccurrence(trackDeliveryResponseDto.getDateTime() <= 0 ? null
                : new DateTimeType(new Date(trackDeliveryResponseDto.getDateTime()), TemporalPrecisionEnum.SECOND));

        return supplyDelivery;
    }

    private OrderInfoDTO getOrderInfo(Long orderId) {
        return  this.genotekService.getDeviceDetails(orderId);
    }

    public void setParticipantAddress(ParticipantDto participantDto) {
        if (participantDto != null && (participantDto.getAddresses() == null
                || participantDto.getAddresses().isEmpty())) {
            UserDTO userDTO = this.apiService.getUserDTO(participantDto.getVibrentID());
            UserAddressDTO userAddressDTO = userDTO.getMailingAddress();
            if (userAddressDTO != null) {
                AddressDto addressDto = new AddressDto();
                addressDto.setLine1(userAddressDTO.getStreetOne());
                addressDto.setLine2(userAddressDTO.getStreetTwo());
                addressDto.setCity(userAddressDTO.getCity());
                addressDto.setPostalCode(userAddressDTO.getZip());
                addressDto.setState(userAddressDTO.getState());
                List<AddressDto> singleAddressList = new ArrayList<>();
                singleAddressList.add(addressDto);
                participantDto.setAddresses(singleAddressList);
            } else {
                log.warn("DRC Service: User does not have mailing address. Participant ID: {}", participantDto.getVibrentID());
            }
        }
    }

/*#############################################################################################################################################

                                                   private methods

##############################################################################################################################################*/

    /**
     * Narrative is required for resource for FHIR
     * This creates default values for narrative
     */
    private static Narrative createDefaultNarrative() {
        Narrative narrative = new Narrative();
        narrative.setStatus(Narrative.NarrativeStatus.GENERATED).setDiv(createDefaultDiv());
        return narrative;
    }

    /**
     * creates a default div for the narrative object
     *
     * @return XhtmlNode
     */
    private static XhtmlNode createDefaultDiv() {
        XhtmlNode defaultNode = new XhtmlNode();
        defaultNode.setContent("default narrative text");
        defaultNode.setNodeType(NodeType.Text);
        return defaultNode;
    }

    /**
     * Get Identifier Type Value for given IdentifierTypeEnum
     * @param type
     * @return
     */
    private static String getIdentifierTypeValue(IdentifierTypeEnum type) {
        switch (type) {
            case ORDER_ID:
                return "orderId";
            case FULFILLMENT_ID:
                return "fulfillmentId";
            case BARCODE_1_D:
                return "barcode";
            case TRACKING_TO_BIOBANK:
            case TRACKING_TO_PARTICIPANT:
                return "trackingId";
            default:
        }
        return null;
    }

    /**
     * takes values and creates and returns identifier
     *
     * @param value       identifier value
     * @param systemValue identifier system value
     * @return new instance identifier that was created
     */
    private Identifier createAndSetIdentifier(String value, String systemValue) {
        Identifier identifier = new Identifier();
        identifier.setSystem(fhirBaseUrl + systemValue);
        identifier.setValue(value);
        return identifier;
    }

    /**
     * creates and returns an device resource
     *
     * @return device
     */
    private Device createDevice(OrderInfoDTO optionalDevice) {
        Device device = new Device();
        //text required for conversion
        device.setText(createDefaultNarrative());
        Identifier sku = new Identifier();
        sku.setSystem(fhirBaseUrl + SKU);
        try {
            sku.setValue(optionalDevice.getItemCode());
            device.addIdentifier(sku);
            List<Device.DeviceDeviceNameComponent> deviceName = new ArrayList<>();
            Device.DeviceDeviceNameComponent deviceDeviceNameComponent = new Device.DeviceDeviceNameComponent();
            deviceDeviceNameComponent.setName(optionalDevice.getItemName());
            deviceDeviceNameComponent.setType(Device.DeviceNameType.MANUFACTURERNAME);
            deviceName.add(deviceDeviceNameComponent);
            device.setDeviceName(deviceName);
            device.setId(DEVICE_ONE);
        } catch (JSONException e) {
            LOGGER.error("Supply conversion device details cannot be parsed", e);
        }
        return device;
    }

    /**
     * creates and returns an organization resource
     *
     * @return organization
     */
    private static Organization createOrganization(String organizationName) {
        Organization organization = new Organization();
        organization.setText(createDefaultNarrative());
        organization.setId(SUPPLIER_ONE);
        organization.setName(organizationName);
        return organization;
    }

    /**
     * takes the common drcDTO
     * creates and returns Patient resource
     *
     * @param participantDto contains information about the participant
     * @return location object
     */
    private Patient createPatient(ParticipantDto participantDto) {
        if (participantDto == null)
            return null;
        Patient patient = new Patient();
        patient.setId(PATIENT_REFERENCE);
        patient.setText(createDefaultNarrative());
        patient.addAddress(convertedAddressToFHIRAddress(participantDto.getAddresses(), AddressUse.HOME));
        patient.addIdentifier(createAndSetIdentifier(participantDto.getExternalID(), SupplyConstants.PARTICIPANT_ID));
        return patient;
    }

    /**
     * takes a generic supply object and parses it situationally
     * take the common drcDTO
     * to create and set the references
     *
     * @param supplyRequest New SupplyRequestOrder being generated
     * @param createTrackOrderResponseDto createTrackOrderResponseDto to convert to supplyRequest
     */
    private static void addSupplyRequestReferences(SupplyRequest supplyRequest, CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        supplyRequest.getRequester().setReference("#" + PATIENT_REFERENCE);
        supplyRequest.setItem(new Reference("#" + DEVICE_ONE));
        //Hard coding it for now.This needs to be modified in the future if we receive the Item quantity from Genotek response
        supplyRequest.setQuantity(new Quantity().setValue(DEFAULT_QUANTITY));

        //Add other references
        supplyRequest.setAuthoredOn(createTrackOrderResponseDto.getDateTime() <= 0L ? null : new Date(createTrackOrderResponseDto.getDateTime()));
        supplyRequest.getDeliverFrom().setReference("#" + SUPPLIER_ONE);
        supplyRequest.getDeliverTo().setReference("#" + PATIENT_REFERENCE);
        supplyRequest.getSupplier().add(new Reference("#" + SUPPLIER_ONE));
    }

    /**
     * return extension based on url and value
     *
     * @param value       value of the extension
     * @param urlEndpoint end part of the full url for extension
     */
    private Extension createExtension(Type value, String urlEndpoint) {
        if (value == null) {
            return null;
        }
        return new Extension().setValue(value).setUrl(fhirBaseUrl + urlEndpoint);
    }

    /**
     * @param userAddress address internal dto
     * @return FHIR Address
     */
    private static Address convertedAddressToFHIRAddress(List<AddressDto> userAddress, AddressUse addressUse) {
        if (userAddress == null || userAddress.isEmpty() || userAddress.get(0) == null) {
            return null;
        }
        Preconditions.checkArgument(userAddress.get(0).getState() != null, "UserAddress State must be provided");

        //use first address
        return new Address().setUse(addressUse)
                .setType(Address.AddressType.POSTAL)
                .addLine(userAddress.get(0).getLine1())
                .addLine(userAddress.get(0).getLine2())
                .setCity(userAddress.get(0).getCity())
                .setState(userAddress.get(0).getState().replace(STATE_PREFIX, ""))
                .setPostalCode(userAddress.get(0).getPostalCode());
    }


    /**
     * takes the common drcDTO
     * creates and returns location resource
     *
     * @param trackDeliveryResponseDto details message of supply delivery
     * @return location object
     */
    private Location createLocation(TrackDeliveryResponseDto trackDeliveryResponseDto, DRCSupplyMessageStatusType statusType) {
        Location location = new Location();
        location.setId(SupplyConstants.LOCATION_ONE);
        AddressDto vxpOrderAddressDTO;
        AddressUse addressUse = AddressUse.HOME;
        if (BIOBANK_SHIPPED.equals(statusType) || BIOBANK_DELIVERY.equals(statusType)) {
            addressUse = AddressUse.WORK;
            vxpOrderAddressDTO = getBioBankAddressDto();

        } else {
            vxpOrderAddressDTO = trackDeliveryResponseDto.getParticipant().getAddresses().get(0);
        }

        if (vxpOrderAddressDTO != null) {
            List<AddressDto> singleAddressList = new ArrayList<>();
            singleAddressList.add(vxpOrderAddressDTO);
            location.setAddress(convertedAddressToFHIRAddress(singleAddressList, addressUse));
        }
        return location;
    }

    private static Long getDateByType(DateTypeEnum type, TrackDeliveryResponseDto trackDeliveryResponseDto){
        if(trackDeliveryResponseDto.getDates() != null) {
            for (DateDto each : trackDeliveryResponseDto.getDates()) {
                if (each.getType().equals(type)) {
                    return each.getDate();
                }
            }
        }
        return null;
    }

    /**
     * takes a generic supply object and parses it situationally
     * take the common drcDTO
     * to create and set the references
     *
     * @param supplyDelivery supply delivery or request
     * @param trackDeliveryResponseDto  track delivery response details
     * @param orderIdentifierDto order Identifier DTO
     */
    private void addSupplyDeliveryReferences(SupplyDelivery supplyDelivery, TrackDeliveryResponseDto trackDeliveryResponseDto, IdentifierDto orderIdentifierDto) {
        if (supplyDelivery == null || trackDeliveryResponseDto == null)
            return;
        supplyDelivery.addBasedOn(new Reference().setIdentifier(createAndSetIdentifier(orderIdentifierDto != null ? orderIdentifierDto.getId() : null, SupplyConstants.ORDER_ID)));
        supplyDelivery.setPatient(new Reference().setIdentifier(createAndSetIdentifier(trackDeliveryResponseDto.getParticipant().getExternalID(), SupplyConstants.PARTICIPANT_ID)));
        supplyDelivery.setSupplier(new Reference("#" + SupplyConstants.SUPPLIER_ONE));
        supplyDelivery.setDestination(new Reference("#" + SupplyConstants.LOCATION_ONE));
    }

    /**
     * takes a generic object and pareses it situationally
     * then creates and sets the supply component object
     * <p>
     * Returns SupplyDeliverySuppliedItemComponent to reference in supply delivery
     */
    private static SupplyDelivery.SupplyDeliverySuppliedItemComponent createSupplyDeliveryComponent(Long quantity) {
        SupplyDelivery.SupplyDeliverySuppliedItemComponent suppliedItem = new SupplyDelivery.SupplyDeliverySuppliedItemComponent();
        suppliedItem.setQuantity(new Quantity(quantity == null ? DEFAULT_QUANTITY : quantity));
        suppliedItem.setItem(new Reference("#" + SupplyConstants.DEVICE_ONE));
        return suppliedItem;
    }


    private void setIdentifierAndExtensionToSupplyRequest(FulfillmentResponseDto fulfillmentResponseDto, SupplyRequest supplyRequest, String orderId) {

        supplyRequest.addIdentifier(createAndSetIdentifier(orderId, getIdentifierTypeValue(IdentifierTypeEnum.ORDER_ID)));

        if (fulfillmentResponseDto.getAttributes().containsKey(IdentifierTypeEnum.FULFILLMENT_ID.toValue())) {
            supplyRequest.addIdentifier(createAndSetIdentifier(fulfillmentResponseDto.getAttributes().get(IdentifierTypeEnum.FULFILLMENT_ID.toValue()), getIdentifierTypeValue(IdentifierTypeEnum.FULFILLMENT_ID)));
        }
        if (fulfillmentResponseDto.getAttributes().containsKey(IdentifierTypeEnum.BARCODE_1_D.toValue())) {
            var barcodeValue = fulfillmentResponseDto.getAttributes().get(IdentifierTypeEnum.BARCODE_1_D.toValue());
            supplyRequest.addExtension(createExtension(barcodeValue == null ? null : new StringType(barcodeValue), getIdentifierTypeValue(IdentifierTypeEnum.BARCODE_1_D)));
        }
    }

    private static void addSupplyRequestReferences(SupplyRequest supplyRequest, FulfillmentResponseDto fulfillmentResponseDto) {
        supplyRequest.setItem(new Reference("#" + DEVICE_ONE));
        supplyRequest.setQuantity(new Quantity().setValue(fulfillmentResponseDto.getOrder().getQuantity()!= null ? fulfillmentResponseDto.getOrder().getQuantity() : DEFAULT_QUANTITY));

        //Add other references
        supplyRequest.setAuthoredOn(new Date(getStatusTime(fulfillmentResponseDto)));

        supplyRequest.getRequester().setReference("#" + PATIENT_REFERENCE);
        supplyRequest.getDeliverFrom().setReference("#" + SUPPLIER_ONE);

        supplyRequest.getSupplier().add(new Reference("#" + SUPPLIER_ONE));
        supplyRequest.getDeliverTo().setReference("#" + PATIENT_REFERENCE);
    }

    private Location createLocation(DRCSupplyMessageStatusType statusType, ParticipantDto participantDto) {
        Location location = new Location();
        location.setId(SupplyConstants.LOCATION_ONE);
        AddressDto vxpOrderAddressDTO = null;
        AddressUse addressUse = AddressUse.HOME;
        if (BIOBANK_SHIPPED.equals(statusType) || BIOBANK_DELIVERY.equals(statusType)) {
            addressUse = AddressUse.WORK;
            vxpOrderAddressDTO = getBioBankAddressDto();

        } else if (participantDto.getAddresses() != null) {
            vxpOrderAddressDTO = participantDto.getAddresses().get(0);
        }

        if (vxpOrderAddressDTO != null) {
            List<AddressDto> singleAddressList = new ArrayList<>();
            singleAddressList.add(vxpOrderAddressDTO);
            location.setAddress(convertedAddressToFHIRAddress(singleAddressList, addressUse));
        }
        return location;
    }

    private AddressDto getBioBankAddressDto() {
        AddressDto vxpOrderAddressDTO = new AddressDto();
        String bioBankAddress = this.apiService.getBioBankAddress();
        try {
            JSONObject json = new JSONObject(bioBankAddress);
            JSONArray jsonLines = json.getJSONArray("line");
            vxpOrderAddressDTO.setLine1(jsonLines.getString(0));
            vxpOrderAddressDTO.setLine1(jsonLines.getString(1));
            vxpOrderAddressDTO.setCity(json.getString("city"));
            vxpOrderAddressDTO.setState(json.getString("state"));
            vxpOrderAddressDTO.setPostalCode(json.getString("postalCode"));
        } catch (JSONException e) {
            LOGGER.error("Supply conversion location details cannot be parsed", e);
        }
        return vxpOrderAddressDTO;
    }

    private void addSupplyDeliveryReferences(SupplyDelivery supplyDelivery, TrackingDetailsDTO trackingDetailsDTO, Long orderId, ParticipantDto participantDto) {
        if (supplyDelivery == null || trackingDetailsDTO == null)
            return;
        supplyDelivery.addBasedOn(new Reference().setIdentifier(createAndSetIdentifier(orderId != null ? String.valueOf(orderId) : null, SupplyConstants.ORDER_ID)));
        supplyDelivery.setPatient(new Reference().setIdentifier(createAndSetIdentifier(participantDto.getExternalID(), SupplyConstants.PARTICIPANT_ID)));
        supplyDelivery.setSupplier(new Reference("#" + SupplyConstants.SUPPLIER_ONE));
        supplyDelivery.setDestination(new Reference("#" + SupplyConstants.LOCATION_ONE));
    }

    private static long getStatusTime(FulfillmentResponseDto fulfillmentResponseDto) {
        return fulfillmentResponseDto.getStatusTime();
    }
}