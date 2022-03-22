package com.vibrent.drc.service.impl;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.exception.RecoverableNetworkException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class DRCBackendProcessorWrapperImplTest {

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Mock
    private DRCBackendProcessorService drcBackendProcessorService;

    private DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    @Before
    public void setUp() throws Exception {
        drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);
    }

    @SneakyThrows
    @Test
    public void testSendRequestReturnDetailsThrowsException() {;
        String url = "url";
        String requestBody = "requestBody";
        RequestMethod requestMethod = RequestMethod.GET;

        Assertions.assertThrows(NullPointerException.class, ()->
                drcBackendProcessorWrapper.sendRequestReturnDetails(url, requestBody, requestMethod, null,  null));

        doThrow(new RecoverableNetworkException("")).when(drcBackendProcessorService).sendRequestReturnDetails(anyString(), nullable(String.class), any(RequestMethod.class), nullable(Map.class));

        Assertions.assertThrows(RecoverableNetworkException.class, ()->
                drcBackendProcessorWrapper.sendRequestReturnDetails(url, requestBody, requestMethod, null,  new ExternalApiRequestLog()));


        doThrow(new DrcConnectorException("")).when(drcBackendProcessorService).sendRequestReturnDetails(anyString(), nullable(String.class), any(RequestMethod.class), nullable(Map.class));

        Assertions.assertThrows(DrcConnectorException.class, ()->
                drcBackendProcessorWrapper.sendRequestReturnDetails(url, requestBody, requestMethod, null,  new ExternalApiRequestLog()));


        doThrow(new Exception("")).when(drcBackendProcessorService).sendRequestReturnDetails(anyString(), nullable(String.class), any(RequestMethod.class), nullable(Map.class));

        Assertions.assertThrows(DrcException.class, ()->
                drcBackendProcessorWrapper.sendRequestReturnDetails(url, requestBody, requestMethod, null,  new ExternalApiRequestLog()));
    }
}
