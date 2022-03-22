package com.vibrent.drc.service;

import com.vibrent.drc.exception.DrcException;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * service for various operations for supplyRequest and supplyDelivery
 */
public interface DRCSupplyStatusService {

    /**
     * get a new participant id so we can associate it to a user that just registered
     *
     * @param supplyMessage message to send to DRC (FHIR)
     * @param userId        user id associated with message
     * @param participantId participant id associated with message
     * @param requestMethod method to use
     * @param fullurl       url to use to call next endpoint
     */
    HttpResponseWrapper sendSupplyStatus(String supplyMessage, Long userId, String participantId, RequestMethod requestMethod, String fullurl, String statusDescription, List<Class<? extends Exception>> retryExceptionsList) throws DrcException;

}
