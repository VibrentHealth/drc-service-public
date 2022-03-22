package com.vibrent.drc.service;

import com.vibrent.vxp.workflow.CreateTrackOrderResponseDto;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryResponseDto;

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
}
