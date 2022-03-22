package com.vibrent.drc.repository;

import com.vibrent.drc.domain.OrderTrackingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTrackingDetailsRepository extends JpaRepository<OrderTrackingDetails, Long> {
    OrderTrackingDetails findByIdentifier(String identifier);

    OrderTrackingDetails findByOrderIdAndIdentifierType(long orderId, OrderTrackingDetails.IdentifierType identifierType);

    OrderTrackingDetails findByIdentifierAndIdentifierType(String identifierId, OrderTrackingDetails.IdentifierType identifierType);

}
