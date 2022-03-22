package com.vibrent.drc.integration;

import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.repository.OrderTrackingDetailsRepository;
import com.vibrent.drc.service.OrderTrackingDetailsService;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.UUID;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
class OrderTrackingDetailsServiceImplTest extends IntegrationTest {

    public static final long ORDER_ID = 1211L;
    @Autowired
    OrderTrackingDetailsService orderTrackingDetailsService;

    @Autowired
    OrderTrackingDetailsRepository orderTrackingDetailsRepository;

    @Test
    void testDetermineSupplyStatusMethodTypeReturn() {
        //Verify Post return if lastMessageStatus is null
        OrderTrackingDetails orderTrackingDetails = insertOrderTrackingDetails(UUID.randomUUID().toString(),
                OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID, null);

        Assertions.assertEquals(RequestMethod.POST,
                orderTrackingDetailsService.determineSupplyStatusMethodType(orderTrackingDetails.getIdentifier()));

        //Verify PUT return if lastMessageStatus is not null
        orderTrackingDetails = insertOrderTrackingDetails(UUID.randomUUID().toString(),
                OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID, "PARTICIPANT_SHIPPED");
        Assertions.assertEquals(RequestMethod.PUT,
                orderTrackingDetailsService.determineSupplyStatusMethodType(orderTrackingDetails.getIdentifier()));


        //Verify POST return if lastMessageStatus is not null
        orderTrackingDetails = insertOrderTrackingDetails(UUID.randomUUID().toString(),
                OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID, null);
        Assertions.assertEquals(RequestMethod.POST,
                orderTrackingDetailsService.determineSupplyStatusMethodType(orderTrackingDetails.getIdentifier()));
    }

    @Test
    void testGetIdentifierIdRequest() {

        OrderTrackingDetails participantTracking = insertOrderTrackingDetails(UUID.randomUUID().toString(),
                OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID, "PARTICIPANT_SHIPPED");

        OrderTrackingDetails returnTracking = insertOrderTrackingDetails(UUID.randomUUID().toString(),
                OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID, "BIOBANK_SHIPPED");

        String tracking = orderTrackingDetailsService.getIdentifierId(ORDER_ID, OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID);
        Assertions.assertEquals(participantTracking.getIdentifier(), tracking);


        tracking = orderTrackingDetailsService.getIdentifierId(ORDER_ID, OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID);
        Assertions.assertEquals(returnTracking.getIdentifier(), tracking);

        Assertions.assertNull(orderTrackingDetailsService.getIdentifierId(1277L, OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID));
    }

    private OrderTrackingDetails insertOrderTrackingDetails(String identifier,
                                                            OrderTrackingDetails.IdentifierType type,
                                                            String lastMessageStatus) {
        OrderTrackingDetails details = new OrderTrackingDetails();
        details.setOrderId(ORDER_ID);
        details.setParticipantId("P101");
        details.setUserId(101L);
        details.setIdentifierType(type);
        details.setIdentifier(identifier);
        details.setLastMessageStatus(lastMessageStatus);

        return orderTrackingDetailsRepository.save(details);
    }
}