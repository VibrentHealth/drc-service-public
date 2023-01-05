package com.vibrent.drc.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.service.DRCParticipantService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountInfoUpdateEventListenerTest {

    private static final String EXTERNAL_ID = "P1232322";
    private static final long VIBRENT_ID = 1000L;

    @Mock
    private AccountInfoUpdateEventService accountInfoUpdateEventService;

    @Mock
    private DRCParticipantService participantService;

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    private AccountInfoUpdateEventListener accountInfoUpdateEventListener;

    private AccountInfoUpdateEventDto accountInfoUpdateEventDto;

    @BeforeEach
    void setUp() {
        String topicName = "event.vxp.push.participant";
        accountInfoUpdateEventListener = new AccountInfoUpdateEventListener(accountInfoUpdateEventService, externalApiRequestLogsService, topicName, participantService);
        initializeAccountInfoUpdateEvent();
    }

    @Test
    @DisplayName("When accountInfoUpdateEventDto is received then the event is processed and external event is sent")
    void listen() throws Exception {
        var accountInfoUpdateEventPayload = buildPayload(accountInfoUpdateEventDto);
        accountInfoUpdateEventListener.listen(accountInfoUpdateEventPayload, buildMessageHeaders());
        verify(externalApiRequestLogsService, times(1)).send(any(ExternalApiRequestLog.class));
        verify(accountInfoUpdateEventService, times(1)).processAccountInfoUpdates(accountInfoUpdateEventDto);
        verify(participantService, times(1)).patchTestParticipant(accountInfoUpdateEventDto);
    }

    @Test
    @DisplayName("When invalid payload received then exception should get logged")
    void testListenOnException() throws Exception {
        var accountInfoUpdateEventPayload = buildInvalidPayload(getUserAccountAddress());
        accountInfoUpdateEventListener.listen(accountInfoUpdateEventPayload, buildMessageHeaders());
        verify(externalApiRequestLogsService, times(0)).send(any(ExternalApiRequestLog.class));
        verify(accountInfoUpdateEventService, times(0)).processAccountInfoUpdates(accountInfoUpdateEventDto);
        verify(participantService, times(0)).patchTestParticipant(accountInfoUpdateEventDto);

        accountInfoUpdateEventListener.listen(null, buildMessageHeaders());
        verify(externalApiRequestLogsService, times(0)).send(any(ExternalApiRequestLog.class));
        verify(accountInfoUpdateEventService, times(0)).processAccountInfoUpdates(accountInfoUpdateEventDto);
        verify(participantService, times(0)).patchTestParticipant(accountInfoUpdateEventDto);
    }

    private AddressElementDto getUserAccountAddress() {

        AddressElementDto addressDto = new AddressElementDto();
        addressDto.setCity("Hagatña");
        addressDto.setLine1("506Hudson");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        addressDto.setAddressType(AddressTypeEnum.ACCOUNT_ADDRESS);

        return addressDto;
    }

    private void initializeAccountInfoUpdateEvent() {
        accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID);

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID(EXTERNAL_ID);
        participantDto.setVibrentID(VIBRENT_ID);
        accountInfoUpdateEventDto.setParticipant(participantDto);
    }

    private MessageHeaders buildMessageHeaders(){
        Map<String, Object> headers = new HashMap<>();
        MessageHeaders messageHeaders;
        headers.put(KAFKA_HEADER_MESSAGE_SPEC, MessageSpecificationEnum.ACCOUNT_INFORMATION_UPDATE.toValue());
        headers.put(KAFKA_HEADER_REPLY_TO_ID, "inReplyTo");
        headers.put(KAFKA_HEADER_MESSAGE_ID, "messageId");
        headers.put(KAFKA_HEADER_MESSAGE_SPEC_VERSION, "version");
        headers.put(KAFKA_HEADER_MESSAGE_TIMESTAMP, 12345678L);
        headers.put(KAFKA_HEADER_PATTERN, IntegrationPatternEnum.PUSH.toValue());
        headers.put(KAFKA_HEADER_PROGRAM_ID, 2L);
        headers.put(KAFKA_HEADER_TENANT_ID, 1L);
        headers.put(KAFKA_HEADER_TRIGGER, ContextTypeEnum.EVENT.toValue());
        headers.put(KAFKA_HEADER_WORKFLOW_INSTANCE_ID, "instanceID");
        headers.put(KAFKA_HEADER_VERSION, "2.1.4");
        headers.put(KAFKA_HEADER_WORKFLOWNAME, WorkflowNameEnum.ACCOUNT_INFORMATION_UPDATE.toValue());
        headers.put(KAFKA_HEADER_ORIGINATOR, RequestOriginatorEnum.VXPMS.toValue());

        messageHeaders = new MessageHeaders(headers);
        return messageHeaders;
    }

    private byte[] buildPayload(AccountInfoUpdateEventDto accountInfoUpdateEventDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(accountInfoUpdateEventDto);
    }
    private byte[] buildInvalidPayload(AddressElementDto accountInfoUpdateEventDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(accountInfoUpdateEventDto);
    }
}