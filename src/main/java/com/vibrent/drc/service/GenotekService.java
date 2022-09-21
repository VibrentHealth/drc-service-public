package com.vibrent.drc.service;

import com.vibrent.genotek.vo.OrderInfoDTO;

public interface GenotekService {

    /**
     * This method fetch device details from Genotek service.
     */
    OrderInfoDTO getDeviceDetails(Long orderId);
}
