package com.vibrent.drc.service;

import com.vibrent.drc.domain.OrderTrackingDetails;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public interface OrderTrackingDetailsService {
    RequestMethod determineSupplyStatusMethodType(String identifierId);

    OrderTrackingDetails getOrderDetails(String identifierId);

    String getIdentifierId(long orderId, OrderTrackingDetails.IdentifierType identifierType);

    void save(OrderTrackingDetails orderTrackingDetails);

    void save(List<OrderTrackingDetails> orderTrackingDetails);

    OrderTrackingDetails getOrderDetails(String identifierId, OrderTrackingDetails.IdentifierType identifierType);
}
