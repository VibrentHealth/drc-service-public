package com.vibrent.drc.service.impl;

import com.google.api.client.http.HttpHeaders;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.DRCSupplyStatusService;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.inject.Inject;
import java.util.List;

/**
 * This class will handle sending SupplyRequestOrder and SupplyDeliveryOrder updates to DRC for any order type
 */
@Slf4j
@Service
public class DRCSupplyStatusServiceImpl implements DRCSupplyStatusService {

    private final DRCRetryService retryService;
    private final DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(DRCSupplyStatusServiceImpl.class);

    @Inject
    public DRCSupplyStatusServiceImpl(DRCRetryService drcRetryService, DRCBackendProcessorWrapper drcBackendProcessorWrapper) {
        this.retryService = drcRetryService;
        this.drcBackendProcessorWrapper = drcBackendProcessorWrapper;
    }

    @Override
    public HttpResponseWrapper sendSupplyStatus(String supplyMessage, Long userId, String participantId, RequestMethod requestMethod, String fullurl, String statusDescription, List<Class<? extends Exception>> retryExceptionsList) throws DrcException {
        if (participantId == null || supplyMessage == null || requestMethod == null || fullurl == null) {
            throw new BusinessValidationException("Attempted to call DRCSupplyStatusServiceImpl with missing parameters");
        }
        if (!drcBackendProcessorWrapper.isInitialized()) {
            return null;
        }

        try {
            return retryService.executeWithRetryForExceptions(() -> sendSupplyStatusRequest(supplyMessage, userId, participantId, requestMethod, fullurl, statusDescription), retryExceptionsList);
        } catch (Exception e) {
            throw new DrcException(e.getMessage(), e);
        }
    }

    /* -------------------------------------------------------------------- */
    /*     private functions                                                */
    /* -------------------------------------------------------------------- */

    private HttpResponseWrapper sendSupplyStatusRequest(String supplyMessage, Long userId, String participantId, RequestMethod requestMethod, String fullurl, String statusDescription) throws Exception {
        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_SUPPLY_STATUS, userId, participantId, statusDescription);
        HttpResponseWrapper response = drcBackendProcessorWrapper.sendRequestReturnDetails(fullurl, supplyMessage, requestMethod, null, externalApiRequestLog);

        HttpHeaders headers = response.getHttpHeaders();
        //We expect DRC to send location header if POST was called
        if (requestMethod.equals(RequestMethod.POST) && (headers == null || headers.getLocation() == null)) {
            LOGGER.error("DRCSupplyStatusService failed to obtain location header after POST response");
            throw new DrcException("DRCSupplyStatusService failed to obtain location header after POST response");
        }
        return response;
    }
}
