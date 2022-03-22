package com.vibrent.drc.util;

import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.vxp.workflow.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class OrderStatusUtilTest {

    private OrderTrackingDetails orderTrackingDetails;

    @BeforeEach
    void setUp() {
        initializeOrderTrackingDetails();
    }

    @Test
    public void testWhenOrderDetailsIsNull() {
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.CREATED, null));
    }

    @Test
    public void testWhenOrderDetailsStatusIsNull() {
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.CREATED, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsCreatedAndStatusIsFulfilled() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.CREATED.toValue());
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.PENDING_SHIPMENT, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsCreatedAndStatusIsCreated() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.CREATED.toValue());
        assertFalse(OrderStatusUtil.isStatusAfter(StatusEnum.CREATED, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsUnknownAndStatusIsCreated() {
        orderTrackingDetails.setLastMessageStatus("Unknown");
        assertFalse(OrderStatusUtil.isStatusAfter(StatusEnum.CREATED, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsShippedAndStatusIsFulfillment() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.SHIPPED.toValue());
        assertFalse(OrderStatusUtil.isStatusAfter(StatusEnum.PENDING_SHIPMENT, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsPendingShipmentAndStatusIsDelivered() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.PENDING_SHIPMENT.toValue());
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.DELIVERED, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsERRORAndStatusIsDelivered() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.ERROR.toValue());
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.DELIVERED, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsERRORAndStatusIsError() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.ERROR.toValue());
        assertFalse(OrderStatusUtil.isStatusAfter(StatusEnum.ERROR, orderTrackingDetails));
    }

    @Test
    public void testWhenOrderDetailsStatusIsPendingShipmentAndStatusIsError() {
        orderTrackingDetails.setLastMessageStatus(StatusEnum.PENDING_SHIPMENT.toValue());
        assertTrue(OrderStatusUtil.isStatusAfter(StatusEnum.ERROR, orderTrackingDetails));
    }

    private void initializeOrderTrackingDetails() {
        orderTrackingDetails = new OrderTrackingDetails();
    }
}