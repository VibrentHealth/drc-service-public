package com.vibrent.drc.service.impl;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.exception.RecoverableNetworkException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class DRCBackendProcessorWrapperImpl implements DRCBackendProcessorWrapper {

    public static final String ERROR_MSG_FAILED_TO_SEND_REQUEST_TO_DRC = "DRC-Service: Failed to send {} request to url {}. Error: {}";

    private final ExternalApiRequestLogsService externalApiRequestLogsService;
    private final DRCBackendProcessorService drcBackendProcessorService;

    @Override
    public boolean isInitialized() {
        return drcBackendProcessorService.isInitialized();
    }

    @Override
    public void initialize(boolean flag) {
        try {
            drcBackendProcessorService.initialize(flag);
        } catch (Exception e) {
            log.warn("DRC Service: DRC Failed to initialize - {}", e.getMessage(), e);
        }
    }

    public HttpResponseWrapper sendRequestReturnDetails(String url, String requestBody, RequestMethod requestMethod, Map<String, String> headers, @NonNull ExternalApiRequestLog externalApiRequestLog) throws DrcException, DrcConnectorException, RecoverableNetworkException {

        HttpResponseWrapper httpResponseWrapper = null;
        Exception exception = null;
        long requestTime = System.currentTimeMillis();
        ExternalApiRequestLogUtil.updateExternalAPILogRequestParams(externalApiRequestLog, requestMethod, url, headers, requestBody, requestTime);

        try {
            httpResponseWrapper = drcBackendProcessorService.sendRequestReturnDetails(url, requestBody, requestMethod, headers);
        } catch (DrcConnectorException | RecoverableNetworkException e) {
            exception = e;
            log.warn(ERROR_MSG_FAILED_TO_SEND_REQUEST_TO_DRC, requestMethod, url, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            exception = e;
            log.warn(ERROR_MSG_FAILED_TO_SEND_REQUEST_TO_DRC, requestMethod, url, e.getMessage(), e);
            throw new DrcException("Failed to send request to DRC", e);
        } finally {
            externalApiRequestLogsService.send(ExternalApiRequestLogUtil.updateExternalAPILogResponseParams(externalApiRequestLog, httpResponseWrapper, System.currentTimeMillis(), exception));
        }
        return httpResponseWrapper;
    }


    public HttpResponseWrapper sendRequest(String url, String requestBody, RequestMethod requestMethod, Map<String, String> headers, @NonNull ExternalApiRequestLog externalApiRequestLog) throws DrcConnectorException {

        HttpResponseWrapper httpResponseWrapper = null;
        Exception exception = null;
        long requestTime = System.currentTimeMillis();
        ExternalApiRequestLogUtil.updateExternalAPILogRequestParams(externalApiRequestLog, requestMethod, url, headers, requestBody, requestTime);

        try {
            httpResponseWrapper = drcBackendProcessorService.sendRequest(url, requestBody, requestMethod, headers);
        } catch (DrcConnectorException e) {
            exception = e;
            log.warn(ERROR_MSG_FAILED_TO_SEND_REQUEST_TO_DRC, requestMethod, url, e.getMessage(), e);
            throw e;
        } finally {
            externalApiRequestLogsService.send(ExternalApiRequestLogUtil.updateExternalAPILogResponseParams(externalApiRequestLog, httpResponseWrapper, System.currentTimeMillis(), exception));
        }
        return httpResponseWrapper;
    }
}
