package com.vibrent.drc.util;

import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VxpPushMessageHeadersUtilTest {

    @Test
    public void testBuildVxpPushMessageHeaderDto() {
        MessageHeaderDto messageHeaderDto = VxpPushMessageHeadersUtil.buildVxpPushMessageHeaderDto(buildMessageHeaders());
        assertNotNull(messageHeaderDto);
        assertEquals(MessageSpecificationEnum.ACCOUNT_INFORMATION_UPDATE, messageHeaderDto.getVxpMessageSpec());
        assertEquals("inReplyTo", messageHeaderDto.getVxpInReplyToID());
        assertEquals("messageId", messageHeaderDto.getVxpMessageID());
        assertEquals("version", messageHeaderDto.getVxpMessageSpecVersion());
        assertEquals(12345678L, messageHeaderDto.getVxpMessageTimestamp());
        assertEquals(IntegrationPatternEnum.PUSH, messageHeaderDto.getVxpPattern());
        assertEquals(2L, messageHeaderDto.getVxpProgramID());
        assertEquals(1L, messageHeaderDto.getVxpTenantID());
        assertEquals(ContextTypeEnum.EVENT, messageHeaderDto.getVxpTrigger());
        assertEquals("instanceID", messageHeaderDto.getVxpWorkflowInstanceID());
        assertEquals("2.1.4", messageHeaderDto.getVxpHeaderVersion());
        assertEquals(WorkflowNameEnum.ACCOUNT_INFORMATION_UPDATE, messageHeaderDto.getVxpWorkflowName());
        assertEquals(RequestOriginatorEnum.VXPMS, messageHeaderDto.getVxpOriginator());
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
}