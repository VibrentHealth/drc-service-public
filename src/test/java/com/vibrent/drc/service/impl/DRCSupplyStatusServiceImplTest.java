package com.vibrent.drc.service.impl;

import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.DRCSupplyStatusService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DRCSupplyStatusServiceImplTest {

    private DRCSupplyStatusService drcSupplyStatusService;
    private Long vibrentId = 1000L;
    private String participantId = "P102937983";

    @Mock
    private DRCBackendProcessorService drcBackendProcessorService;

    @Mock
    private DRCConfigService drcConfigService;

    @Mock
    private DRCRetryService retryService;

    @Mock
    ExternalApiRequestLogsService externalApiRequestLogsService;

    @BeforeEach
    void setUp() {
        DRCBackendProcessorWrapper drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);
        drcSupplyStatusService = new DRCSupplyStatusServiceImpl(retryService, drcBackendProcessorWrapper);
    }

    @Test
    @DisplayName("When SupplyMessage is Null then BusinessValidationException is thrown")
    void testSendSupplyStatusWhenSupplyMessageIsNull() {
        assertThrows(BusinessValidationException.class,
                () -> drcSupplyStatusService.sendSupplyStatus(null, vibrentId, participantId, RequestMethod.POST, "fullUrl", "", new ArrayList<>()));
    }

    @Test
    @DisplayName("When ParticipantId is Null then BusinessValidationException is thrown")
    void testSendSupplyStatusWhenParticipantIdIsNull() {
        assertThrows(BusinessValidationException.class,
                () -> drcSupplyStatusService.sendSupplyStatus("SupplyMessage", vibrentId, null, RequestMethod.POST, "fullUrl", "", new ArrayList<>()));
    }

    @Test
    @DisplayName("When RequestMethod is Null then BusinessValidationException is thrown")
    void testSendSupplyStatusWhenRequestMethodIsNull() {
        assertThrows(BusinessValidationException.class,
                () -> drcSupplyStatusService.sendSupplyStatus("SupplyMessage", vibrentId, participantId, null, "fullUrl", "", new ArrayList<>()));
    }

    @Test
    @DisplayName("When fullUrl is Null then BusinessValidationException is thrown")
    void testSendSupplyStatusWhenFullUrlIsNull() {
        assertThrows(BusinessValidationException.class,
                () -> drcSupplyStatusService.sendSupplyStatus("SupplyMessage", vibrentId, participantId, RequestMethod.POST, null, "", new ArrayList<>()));
    }

    @Test
    @DisplayName("When drcBackendProcessorService is not initialized then DRC call is not made")
    void testSendSupplyStatusWhenDrcBackendProcessorServiceIsNotInitialized() throws Exception {
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(false);
        drcSupplyStatusService.sendSupplyStatus("SupplyMessage", vibrentId, participantId, RequestMethod.POST, "fullUrl", "", new ArrayList<>());
        Mockito.verify(drcBackendProcessorService, Mockito.times(0)).sendRequestReturnDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestMethod.class),
                Mockito.anyMap());
    }

    @Test
    @DisplayName("When drcBackendProcessorService is initialized then request is processed")
    void testSendSupplyStatusWhenDrcBackendProcessorServiceIsInitialized() throws Exception {
        Mockito.when(drcBackendProcessorService.isInitialized()).thenReturn(true);
        drcSupplyStatusService.sendSupplyStatus("SupplyMessage", vibrentId, participantId, RequestMethod.POST, "fullUrl", "", new ArrayList<>());
        Mockito.verify(retryService, Mockito.times(1)).executeWithRetryForExceptions(Mockito.any(), Mockito.anyList());
    }
}