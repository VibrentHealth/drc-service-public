package com.vibrent.drc.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.dto.Participant;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.AccountInfoUpdateEventHelperService;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import com.vibrent.vxp.push.ParticipantDto;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DRCParticipantServiceImplTest {

    private static final String PARTICIPANT_ID = "P1";
    private static final long USER_ID = 1L;
    private DRCParticipantServiceImpl drcParticipantService;

    @Mock
    private DRCRetryService retryService;

    @Mock
    private DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    @Mock
    private DrcProperties drcProperties;

    @Mock
    private AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService;

    @BeforeEach
    public void setUp() throws Exception {
        drcParticipantService = new DRCParticipantServiceImpl(retryService, drcBackendProcessorWrapper, drcProperties, accountInfoUpdateEventHelperService);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventIsNull() {
        ListAppender<ILoggingEvent> listAppender = getLoggingEventListAppender();
        drcParticipantService.validateAndCallDrcEndpoint(null);
        verifyIfEventIsNotValid(listAppender);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventExternalIdIsNullOrEmpty() {
        ListAppender<ILoggingEvent> listAppender = getLoggingEventListAppender();
        drcParticipantService.validateAndCallDrcEndpoint(new AccountInfoUpdateEventDto());
        verifyIfEventIsNotValid(listAppender);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventParticipantIsNull() {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID("external-id-1");

        ListAppender<ILoggingEvent> listAppender = getLoggingEventListAppender();
        drcParticipantService.validateAndCallDrcEndpoint(accountInfoUpdateEventDto);
        verifyIfEventIsNotValid(listAppender);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventParticipantTestFlagIsNull() {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID("external-id-1");
        accountInfoUpdateEventDto.setParticipant(new ParticipantDto());

        ListAppender<ILoggingEvent> listAppender = getLoggingEventListAppender();
        drcParticipantService.validateAndCallDrcEndpoint(accountInfoUpdateEventDto);
        verifyIfEventIsNotValid(listAppender);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventParticipantTestFlagIsFalse() {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setTestUser(false);
        accountInfoUpdateEventDto.setExternalID("external-id-1");
        accountInfoUpdateEventDto.setParticipant(participantDto);

        ListAppender<ILoggingEvent> listAppender = getLoggingEventListAppender();
        drcParticipantService.validateAndCallDrcEndpoint(accountInfoUpdateEventDto);
        verifyIfEventIsNotValid(listAppender);
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventIsValid(){
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = getValidAccountInfoUpdateEventDto();
        drcParticipantService.patchTestParticipant(accountInfoUpdateEventDto);
        verify(accountInfoUpdateEventHelperService, times(1)).processIfTestParticipantUpdated(any(AccountInfoUpdateEventDto.class), any(BooleanSupplier.class));
    }

    @Test
    void patchTestParticipantWhenAccountInfoUpdateEventIsValidAndRetryServiceThrownException() throws Exception {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = getValidAccountInfoUpdateEventDto();
        doThrow(new Exception()).when(retryService).executeWithRetry(any());
        assertFalse(drcParticipantService.validateAndCallDrcEndpoint(accountInfoUpdateEventDto));
    }

    @Test
    void testGetParticipantWhenParticipantIdIsNullOrEmpty() throws Exception {
        assertNull(drcParticipantService.getParticipantById(USER_ID, null));
        assertNull(drcParticipantService.getParticipantById(USER_ID, ""));
    }

    @Test
    void testGetParticipantWhenParticipantIdIsValid() throws Exception {
        when(drcBackendProcessorWrapper.sendRequest(anyString(), any(), any(RequestMethod.class), any(), any(ExternalApiRequestLog.class)))
                .thenReturn(getParticipantResponse(false));
        Participant participant = drcParticipantService.getParticipantById(USER_ID, PARTICIPANT_ID);
        assertNotNull(participant);
        assertEquals(PARTICIPANT_ID, participant.getParticipantId());
        assertFalse(participant.getTestParticipant());
    }

    @Test
    void testGetParticipantWhenParticipantIdIsValidAndDrcReturnsInValid() throws Exception {
        when(drcBackendProcessorWrapper.sendRequest(anyString(), any(), any(RequestMethod.class), any(), any(ExternalApiRequestLog.class)))
                .thenReturn(new HttpResponseWrapper(400, null));
        assertNull(drcParticipantService.getParticipantById(USER_ID, PARTICIPANT_ID));
    }

    @Test
    void testPatchParticipant() throws Exception {
        when(drcBackendProcessorWrapper.sendRequest(anyString(), any(), any(RequestMethod.class), any(), any(ExternalApiRequestLog.class)))
                .thenReturn(getParticipantResponse(false));

        assertTrue(drcParticipantService.patchParticipantInternal(USER_ID, getParticipant(true)));
        verify(drcBackendProcessorWrapper, times(1)).sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), anyMap(), any(ExternalApiRequestLog.class));
    }

    @Test
    void testPatchParticipantWhenParticipantByIdReturnsNull() throws Exception {
        when(drcBackendProcessorWrapper.sendRequest(anyString(), any(), any(RequestMethod.class), any(), any(ExternalApiRequestLog.class)))
                .thenReturn(new HttpResponseWrapper(400, null));

        assertFalse(drcParticipantService.patchParticipantInternal(USER_ID, getParticipant(true)));
        verify(drcBackendProcessorWrapper, times(0)).sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), anyMap(), any(ExternalApiRequestLog.class));
    }

    private ListAppender<ILoggingEvent> getLoggingEventListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(DRCParticipantServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        return listAppender;
    }

    private void verifyIfEventIsNotValid(ListAppender<ILoggingEvent> listAppender) {
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: UserId is null or participantId is null or test user flag is null or false. Cannot patch participant with DRC", logsList.get(0).getMessage());
    }

    private AccountInfoUpdateEventDto getValidAccountInfoUpdateEventDto() {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setTestUser(true);

        accountInfoUpdateEventDto.setExternalID(PARTICIPANT_ID);
        accountInfoUpdateEventDto.setParticipant(participantDto);
        return accountInfoUpdateEventDto;
    }

    private HttpResponseWrapper getParticipantResponse(boolean status) throws JsonProcessingException {
        Participant participant = getParticipant(status);
        return new HttpResponseWrapper(200, JacksonUtil.getMapper().writeValueAsString(participant));
    }

    private Participant getParticipant(boolean status) {
        Participant participant = new Participant();
        participant.setParticipantId(PARTICIPANT_ID);
        participant.setTestParticipant(status);
        return participant;
    }
}