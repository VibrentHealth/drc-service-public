package com.vibrent.drc.util;

import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.enumeration.OrderTrackingStatusEnum;
import com.vibrent.vxp.workflow.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class OrderStatusUtil {

    private OrderStatusUtil() {
    }

    public static boolean isStatusAfter(StatusEnum statusEnum, OrderTrackingDetails orderTrackingDetails) {
        if (statusEnum == null || orderTrackingDetails == null || StringUtils.isEmpty(orderTrackingDetails.getLastMessageStatus())) {
            return true;
        }
        try {
            OrderTrackingStatusEnum currentStatus = OrderTrackingStatusEnum.valueOf(statusEnum.toValue());
            OrderTrackingStatusEnum previousStatus = OrderTrackingStatusEnum.valueOf(orderTrackingDetails.getLastMessageStatus());
            //If previous status is ERROR and received any other status after that then sending as true so that it will be notified to DRC
            if (previousStatus == OrderTrackingStatusEnum.ERROR && previousStatus != currentStatus) {
                return true;
            } else if (currentStatus.compareTo(previousStatus) > 0) {
                return true;
            } else {
                log.info("DRC Service: Order Status received is : {} before or equal to database status: {}", currentStatus, previousStatus);
            }
        } catch (Exception e) {
            log.warn("DRC Service: Error while converting status to OrderTrackingStatusEnum", e);
        }
        return false;
    }
}
