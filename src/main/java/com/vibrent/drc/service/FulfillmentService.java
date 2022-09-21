package com.vibrent.drc.service;

import com.vibrent.drc.exception.DrcException;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;

public interface FulfillmentService {

    /**
     * This method fetch order details from Fulfillment service.
     */
    OrderDetailsDTO getOrderById(Long orderId) throws DrcException;
}
