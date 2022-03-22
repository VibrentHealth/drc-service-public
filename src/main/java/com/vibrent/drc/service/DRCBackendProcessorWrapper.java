package com.vibrent.drc.service;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.exception.DrcException;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.exception.RecoverableNetworkException;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

public interface DRCBackendProcessorWrapper {

    boolean isInitialized();

    void initialize(boolean flag);

    HttpResponseWrapper sendRequestReturnDetails(String url, String requestBody, RequestMethod requestMethod, Map<String, String> headers, ExternalApiRequestLog externalApiRequestLog)
            throws DrcException, DrcConnectorException, RecoverableNetworkException;

    HttpResponseWrapper sendRequest(String url, String requestBody, RequestMethod requestMethod, Map<String, String> headers, ExternalApiRequestLog externalApiRequestLog) throws DrcConnectorException;
}
