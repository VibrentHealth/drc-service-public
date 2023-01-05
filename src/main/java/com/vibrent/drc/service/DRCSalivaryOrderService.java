package com.vibrent.drc.service;

import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.vxp.workflow.*;

/**
 * service for various operations for supplyRequest and supplyDelivery
 */
public interface DRCSalivaryOrderService {

    /**
     * Verify and send the response received from Genotek
     *
     * @param createTrackOrderResponseDto Response object received from Genotek
     * @param messageHeaderDto messageHeaderDto received from Genotek response
     */
    void verifyAndSendCreateTrackOrderResponse(CreateTrackOrderResponseDto createTrackOrderResponseDto, MessageHeaderDto messageHeaderDto);

    /**
     * Verify and send the response received from USPS
     *
     * @param trackDeliveryResponseDto Response object received from USPS
     * @param messageHeaderDto messageHeaderDto received from USPS response
     * @return SupplyRequestOrder
     */
    void verifyAndSendTrackDeliveryResponse(TrackDeliveryResponseDto trackDeliveryResponseDto, MessageHeaderDto messageHeaderDto) ;

    /**
     * Verify and send the response received from Fulfillment
     *
     * @param fulfillmentResponseDto Response object received from Fulfillment
     * @param messageHeaderDto       messageHeaderDto received from Fulfillment response
     * @param orderDetailsDTO
     * @param participantDto
     */
    void verifyAndSendFulfillmentOrderResponse(FulfillmentResponseDto fulfillmentResponseDto, MessageHeaderDto messageHeaderDto, OrderDetailsDTO orderDetailsDTO, ParticipantDto participantDto);

}
