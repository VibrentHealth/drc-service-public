package com.vibrent.drc.service.impl;

import ca.uhn.fhir.context.FhirContext;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.enumeration.DRCSupplyMessageStatusType;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.DRCSupplyStatusService;
import com.vibrent.drc.service.OrderTrackingDetailsService;
import com.vibrent.vxp.workflow.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.RecoverableNetworkException;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.vibrent.drc.constants.DrcConstant.URL_SUPPLYDELIVERY;
import static com.vibrent.drc.constants.SupplyConstants.URL_SUPPLYREQUEST;
import static com.vibrent.drc.domain.OrderTrackingDetails.IdentifierType.*;
import static com.vibrent.drc.enumeration.DRCSupplyMessageStatusType.*;
import static com.vibrent.vxp.workflow.IdentifierTypeEnum.TRACKING_TO_BIOBANK;
import static com.vibrent.vxp.workflow.IdentifierTypeEnum.TRACKING_TO_PARTICIPANT;


@Slf4j
@Service
public class DRCSalivaryOrderServiceImpl implements DRCSalivaryOrderService {

    private final FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;
    private final DRCConfigService drcConfigService;
    private final DRCSupplyStatusService drcSupplyStatusService;
    private final FhirContext fhirContext;
    private final DrcProperties drcProperties;
    private final OrderTrackingDetailsService orderTrackingDetailsService;


    //Salivary specific exceptions to retry on
    private final List<Class<? extends Exception>> salivaryExceptions = Collections.singletonList(RecoverableNetworkException.class);

    @Inject
    public DRCSalivaryOrderServiceImpl(FHIRSalivaryConverterUtility fhirSalivaryConverterUtility, DRCConfigService drcConfigService,
                                       DRCSupplyStatusService drcSupplyStatusService, DrcProperties drcProperties, OrderTrackingDetailsService orderTrackingDetailsService) {
        this.fhirSalivaryConverterUtility = fhirSalivaryConverterUtility;
        this.drcSupplyStatusService = drcSupplyStatusService;
        this.drcConfigService = drcConfigService;
        this.drcProperties = drcProperties;
        this.orderTrackingDetailsService = orderTrackingDetailsService;
        fhirContext = FhirContext.forR4();
    }

    @Override
    public void verifyAndSendTrackDeliveryResponse(TrackDeliveryResponseDto trackDeliveryResponseDto, MessageHeaderDto messageHeaderDto) {
        log.info("DRC-Service | Received Track Delivery Response VXP message");
        if (trackDeliveryResponseDto == null || messageHeaderDto == null) {
            throw new BusinessValidationException("Unable to continue supply delivery due to missing request");
        }

        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(trackDeliveryResponseDto.getTrackingID());
        if(orderDetails == null) {
            throw new BusinessValidationException("DRCSupplyStatusServiceImpl: Unable to continue supply delivery, order tracking not found for trackingId: " + trackDeliveryResponseDto.getTrackingID());
        }

        DRCSupplyMessageStatusType statusType = determineStatusByDTO(trackDeliveryResponseDto, orderDetails);

        SupplyDelivery.SupplyDeliveryStatus supplyDeliveryStatus = SupplyDelivery.SupplyDeliveryStatus.INPROGRESS;
        String url = URL_SUPPLYDELIVERY;
        String supplyFHIRMessage;

        //Determine if call needs to be POST or PUT
        RequestMethod requestMethod = determineHttpMethodType(orderDetails);
        supplyDeliveryStatus = BIOBANK_DELIVERY.equals(statusType) || PARTICIPANT_DELIVERY.equals(statusType) ? SupplyDelivery.SupplyDeliveryStatus.COMPLETED : supplyDeliveryStatus;

        IdentifierDto orderIdentifierDto = getOrderIdIdentifier(orderDetails.getOrderId());
        SupplyDelivery supplyDelivery = fhirSalivaryConverterUtility.
                orderToSupplyDeliveryFHIRConverter(trackDeliveryResponseDto,
                        supplyDeliveryStatus,
                        statusType, messageHeaderDto, orderIdentifierDto);

        supplyFHIRMessage = fhirContext.newJsonParser().setPrettyPrint(false).setSuppressNarratives(false).setSummaryMode(false).encodeResourceToString(supplyDelivery);

        try {
            String fullurl = appendLiteralIdToURLForTrackDelivery(url, statusType, requestMethod, orderDetails);
            HttpResponseWrapper response = drcSupplyStatusService.sendSupplyStatus(supplyFHIRMessage, trackDeliveryResponseDto.getParticipant().getVibrentID(),
                    trackDeliveryResponseDto.getParticipant().getExternalID(), requestMethod, fullurl, getStatusType(statusType), salivaryExceptions);
            updatedOrderTrackDetails(response, trackDeliveryResponseDto);
            log.info("DRC Service: Successfully sent DRC SupplyDelivery for Participant ID: {}", trackDeliveryResponseDto.getParticipant().getExternalID());
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("DRC request failed:", e);
        }
    }

    @Override
    public void verifyAndSendCreateTrackOrderResponse(CreateTrackOrderResponseDto createTrackOrderResponseDto, MessageHeaderDto messageHeaderDto) {
        if (createTrackOrderResponseDto == null || messageHeaderDto == null) {
            throw new BusinessValidationException("Unable to continue DRC supply request due to missing request");
        }
        String orderId = getOrderId(createTrackOrderResponseDto.getIdentifiers());
        OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(orderId, ORDER_ID);

        DRCSupplyMessageStatusType statusType = determineStatusByDTO(createTrackOrderResponseDto);
        SupplyRequest.SupplyRequestStatus supplyRequestStatus = SupplyRequest.SupplyRequestStatus.ACTIVE;
        validateSupplyRequestDetails(createTrackOrderResponseDto);

        String url = URL_SUPPLYREQUEST;
        String supplyFHIRMessage;

        //Determine if call needs to be POST or PUT
        RequestMethod requestMethod = determineHttpMethodType(orderDetails);

        //update the SR status based on workflow state
        supplyRequestStatus = CANCELLED.equals(statusType) ? SupplyRequest.SupplyRequestStatus.CANCELLED : supplyRequestStatus;
        supplyRequestStatus = SHIPPED.equals(statusType) ? SupplyRequest.SupplyRequestStatus.COMPLETED : supplyRequestStatus;

        //convert objects to FHIR message
        SupplyRequest supplyRequest = fhirSalivaryConverterUtility.orderToSupplyRequestFHIRConverter(createTrackOrderResponseDto, messageHeaderDto, supplyRequestStatus);
        supplyRequest.setAuthoredOn(new Date(createTrackOrderResponseDto.getDateTime()));
        supplyFHIRMessage = fhirContext.newJsonParser().setPrettyPrint(false).setSuppressNarratives(false).setSummaryMode(false).encodeResourceToString(supplyRequest);

        try {
            String fullUrl = appendLiteralIdToURL(url, statusType, createTrackOrderResponseDto, requestMethod);
            //Create Order tracking details
            createOrderTrackingDetails(createTrackOrderResponseDto, orderId);
            HttpResponseWrapper response = drcSupplyStatusService.sendSupplyStatus(supplyFHIRMessage, createTrackOrderResponseDto.getParticipant().getVibrentID(),
                    createTrackOrderResponseDto.getParticipant().getExternalID(), requestMethod, fullUrl, statusType != null ? statusType.toString() : null, salivaryExceptions);
            log.info("DRC Service: Successfully send DRC SupplyRequest for Participant ID: {}", createTrackOrderResponseDto.getParticipant().getExternalID());
            updateOrderTrackingDetails(response, createTrackOrderResponseDto, orderId);
        } catch (BusinessValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("DRC request failed:", e);
        }
    }

    /* -------------------------------------------------------------------- */
    /*     private functions                                                */
    /* -------------------------------------------------------------------- */

    //Updated order tracking details on successful DRC call
    private void updateOrderTrackingDetails(HttpResponseWrapper response, CreateTrackOrderResponseDto createTrackOrderResponseDto, String orderId) {
        OrderTrackingDetails orderTrackingDetails;
        if (response != null && (response.getStatusCode() == 200 || response.getStatusCode() == 201) && orderId != null) {
            orderTrackingDetails = orderTrackingDetailsService.getOrderDetails(orderId, ORDER_ID);
            if (orderTrackingDetails != null) {
                orderTrackingDetails.setLastMessageStatus(createTrackOrderResponseDto.getStatus().toValue());
                orderTrackingDetailsService.save(orderTrackingDetails);
            }
        }
    }

    private void createOrderTrackingDetails(CreateTrackOrderResponseDto createTrackOrderResponseDto, String orderId) {
        OrderTrackingDetails orderTrackingDetails = null;
        List<IdentifierDto> identifiers = createTrackOrderResponseDto.getIdentifiers();
        List<OrderTrackingDetails> orderTrackingDetailsList = new ArrayList<>();
        for (IdentifierDto identifier : identifiers) {
            IdentifierTypeEnum identifierType = identifier.getType();
            if (IdentifierTypeEnum.ORDER_ID.equals(identifierType)) {
                orderTrackingDetails = orderTrackingDetailsService.getOrderDetails(identifier.getId(), ORDER_ID);
                if (orderTrackingDetails == null) {
                    orderTrackingDetails = addOrderTrackingDetails(createTrackOrderResponseDto, identifier, ORDER_ID, orderId);
                    orderTrackingDetailsList.add(orderTrackingDetails);
                }
            } else if (TRACKING_TO_PARTICIPANT.equals(identifierType)) {
                orderTrackingDetails = orderTrackingDetailsService.getOrderDetails(identifier.getId(), PARTICIPANT_TRACKING_ID);
                if (orderTrackingDetails == null) {
                    orderTrackingDetails = addOrderTrackingDetails(createTrackOrderResponseDto, identifier, PARTICIPANT_TRACKING_ID, orderId);
                    orderTrackingDetailsList.add(orderTrackingDetails);
                }
            } else if (TRACKING_TO_BIOBANK.equals(identifierType)) {
                orderTrackingDetails = orderTrackingDetailsService.getOrderDetails(identifier.getId(), RETURN_TRACKING_ID);
                if (orderTrackingDetails == null) {
                    orderTrackingDetails = addOrderTrackingDetails(createTrackOrderResponseDto, identifier, RETURN_TRACKING_ID, orderId);
                    orderTrackingDetailsList.add(orderTrackingDetails);
                }
            }
        }
        orderTrackingDetailsService.save(orderTrackingDetailsList);
    }

    private OrderTrackingDetails addOrderTrackingDetails(CreateTrackOrderResponseDto createTrackOrderResponseDto, IdentifierDto identifier, OrderTrackingDetails.IdentifierType identifierType, String orderId){
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setOrderId(Long.parseLong(orderId));
        orderTrackingDetails.setUserId(createTrackOrderResponseDto.getParticipant().getVibrentID());
        orderTrackingDetails.setParticipantId(createTrackOrderResponseDto.getParticipant().getExternalID());
        orderTrackingDetails.setLastMessageStatus(null);
        //setting Identifier and IdentifierType
        orderTrackingDetails.setIdentifier(identifier.getId());
        orderTrackingDetails.setIdentifierType(identifierType);
        return orderTrackingDetails;
    }

    private void updatedOrderTrackDetails(HttpResponseWrapper response, TrackDeliveryResponseDto trackDeliveryResponseDto) {
        if (response != null && (response.getStatusCode() == 200 || response.getStatusCode() == 201)) {
            OrderTrackingDetails orderDetails = orderTrackingDetailsService.getOrderDetails(trackDeliveryResponseDto.getTrackingID());
            if (orderDetails != null) {
                orderDetails.setLastMessageStatus(trackDeliveryResponseDto.getStatus().toValue());
                orderTrackingDetailsService.save(orderDetails);
            }
        }
    }

    private String getOrderId(List<IdentifierDto> identifiers) {
        String orderId = null;
        for (IdentifierDto identifier : identifiers) {
            if (IdentifierTypeEnum.ORDER_ID.equals(identifier.getType())) {
                orderId = identifier.getId();
            }
        }
        return orderId;
    }

    private static String getStatusType(DRCSupplyMessageStatusType statusType) {
        return statusType != null ? statusType.toString() : null;
    }

    private static IdentifierDto getOrderIdIdentifier(Long orderId) {
        IdentifierDto deliveryOrderId = new IdentifierDto();
        deliveryOrderId.setType(IdentifierTypeEnum.ORDER_ID);
        deliveryOrderId.setProvider(ProviderEnum.GENOTEK);
        deliveryOrderId.setId(orderId.toString());
        return deliveryOrderId;
    }

    private String appendLiteralIdToURLForTrackDelivery(String url, DRCSupplyMessageStatusType statusType, RequestMethod requestMethod, OrderTrackingDetails orderTrackingDetails) {
        if (orderTrackingDetails == null || requestMethod.equals(RequestMethod.POST)) {
            return drcProperties.getDrcApiBaseUrl() + url;
        }

        switch (statusType) {
            case PARTICIPANT_DELIVERY:
            case BIOBANK_DELIVERY:
                return (orderTrackingDetails.getOrderId() == null) ? drcProperties.getDrcApiBaseUrl() + url : drcProperties.getDrcApiBaseUrl() + url + "/" + orderTrackingDetails.getOrderId();
            default:
                return drcProperties.getDrcApiBaseUrl() + url;
        }
    }

    private DRCSupplyMessageStatusType determineStatusByDTO(TrackDeliveryResponseDto trackDeliveryResponseDto, OrderTrackingDetails orderDetails) {
        if (trackDeliveryResponseDto.getTrackingID() == null || trackDeliveryResponseDto.getStatus() == null) {
            log.error("DRC-Service: Unable to retrieve tracking details for supplyDelivery");
            return null;
        }

        if (orderDetails.getIdentifierType().equals(PARTICIPANT_TRACKING_ID)) {
            if (StatusEnum.IN_TRANSIT.equals(trackDeliveryResponseDto.getStatus())) {
                return PARTICIPANT_SHIPPED;
            } else {
                return DRCSupplyMessageStatusType.PARTICIPANT_DELIVERY;
            }
        }

        if (orderDetails.getIdentifierType().equals(RETURN_TRACKING_ID)) {

            if (StatusEnum.IN_TRANSIT.equals(trackDeliveryResponseDto.getStatus())) {
                return DRCSupplyMessageStatusType.BIOBANK_SHIPPED;
            } else {
                return DRCSupplyMessageStatusType.BIOBANK_DELIVERY;
            }
        }
        return null;
    }

    private static DRCSupplyMessageStatusType determineStatusByDTO(CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        if (createTrackOrderResponseDto.getStatus() != null) {
            switch (createTrackOrderResponseDto.getStatus()) {
                case CREATED:
                    return DRCSupplyMessageStatusType.CREATED;
                case PENDING_SHIPMENT:
                    return DRCSupplyMessageStatusType.FULFILLMENT;
                case SHIPPED:
                    return DRCSupplyMessageStatusType.SHIPPED;
                case ERROR:
                    return DRCSupplyMessageStatusType.CANCELLED;
                default:
            }
        }

        if (createTrackOrderResponseDto.getStatus() == null) {
            log.error("DRCSupplyStatusServiceImpl: Unable to retrieve tracking details for supplyDelivery");
            return null;
        }
        return null;
    }

    private static void validateSupplyRequestDetails(CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        //Log supply request if missing info
        if (StatusEnum.SHIPPED.equals(createTrackOrderResponseDto.getStatus())
            && (getIdentifierByType(TRACKING_TO_PARTICIPANT, createTrackOrderResponseDto) == null ||
            getIdentifierByType(TRACKING_TO_BIOBANK, createTrackOrderResponseDto) == null)) {
            log.error("DRCSalivaryOrderService: SupplyRequestOrder shipped came without tracking id's");
        }

        if (StatusEnum.SHIPPED.equals(createTrackOrderResponseDto.getStatus())
            && getIdentifierByType(IdentifierTypeEnum.BARCODE_1_D, createTrackOrderResponseDto) == null) {
            log.error("DRCSalivaryOrderService: SupplyRequestOrder shipped came without barcode");
        }
    }

    private static IdentifierDto getIdentifierByType(IdentifierTypeEnum type, CreateTrackOrderResponseDto createTrackOrderResponseDto){
        if(createTrackOrderResponseDto.getIdentifiers() != null) {
            for (IdentifierDto each : createTrackOrderResponseDto.getIdentifiers()) {
                if (each.getType().equals(type)) {
                    return each;
                }
            }
        }
        return null;
    }

    private String appendLiteralIdToURL(String url, DRCSupplyMessageStatusType statusType, CreateTrackOrderResponseDto trackOrderResponseDto, RequestMethod requestMethod) {
        if (requestMethod.equals(RequestMethod.POST)) {
            return drcConfigService.getDrcApiBaseUrl() + url;
        }
        String orderId = getOrderId(trackOrderResponseDto);

        switch (statusType) {
            case FULFILLMENT:
            case SHIPPED:
            case CANCELLED:
                return drcConfigService.getDrcApiBaseUrl() + url + (orderId != null ? "/" + orderId : "");
            default:
                return drcConfigService.getDrcApiBaseUrl() + url;
        }
    }

    private static String getOrderId(CreateTrackOrderResponseDto trackOrderResponseDto) {
        List<IdentifierDto> identifierDtos = trackOrderResponseDto.getIdentifiers();
        for(IdentifierDto identifierDto : identifierDtos) {
            if(identifierDto.getType() == IdentifierTypeEnum.ORDER_ID) {
                return identifierDto.getId();
            }
        }
        return null;
    }

    private static RequestMethod determineHttpMethodType(OrderTrackingDetails orderTrackingDetails) {
        if (orderTrackingDetails == null || orderTrackingDetails.getLastMessageStatus() == null) {
            return RequestMethod.POST;
        } else {
            return RequestMethod.PUT;
        }
    }

}
