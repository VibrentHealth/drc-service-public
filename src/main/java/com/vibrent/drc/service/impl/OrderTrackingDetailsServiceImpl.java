package com.vibrent.drc.service.impl;

import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.repository.OrderTrackingDetailsRepository;
import com.vibrent.drc.service.OrderTrackingDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Service
public class OrderTrackingDetailsServiceImpl implements OrderTrackingDetailsService {

    final OrderTrackingDetailsRepository orderTrackingDetailsRepository;

    public OrderTrackingDetailsServiceImpl(OrderTrackingDetailsRepository orderTrackingDetailsRepository) {
        this.orderTrackingDetailsRepository = orderTrackingDetailsRepository;
    }

    @Override
    public RequestMethod determineSupplyStatusMethodType(String identifierId) {
        var orderDetails = orderTrackingDetailsRepository.findByIdentifier(identifierId);
        return orderDetails == null || orderDetails.getLastMessageStatus() == null
                ? RequestMethod.POST : RequestMethod.PUT;
    }

    @Override
    public OrderTrackingDetails getOrderDetails(String identifierId) {
        return orderTrackingDetailsRepository.findByIdentifier(identifierId);
    }


    @Override
    public String getIdentifierId(long orderId, OrderTrackingDetails.IdentifierType identifierType) {
        OrderTrackingDetails orderDetails = orderTrackingDetailsRepository.findByOrderIdAndIdentifierType(orderId, identifierType);
        return orderDetails == null ? null : orderDetails.getIdentifier();
    }

    @Override
    public void save(OrderTrackingDetails orderTrackingDetails) {
        orderTrackingDetailsRepository.save(orderTrackingDetails);
    }

    @Override
    public void save(List<OrderTrackingDetails> orderTrackingDetails) {
        if (!CollectionUtils.isEmpty(orderTrackingDetails)) {
            orderTrackingDetailsRepository.saveAll(orderTrackingDetails);
        }
    }

    @Override
    public OrderTrackingDetails getOrderDetails(String identifierId, OrderTrackingDetails.IdentifierType identifierType){
        return orderTrackingDetailsRepository.findByIdentifierAndIdentifierType(identifierId,identifierType);
    }
}
