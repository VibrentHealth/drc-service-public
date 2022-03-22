package com.vibrent.drc.messaging.consumer;

import com.vibrent.vxp.workflow.*;
import org.springframework.messaging.MessageHeaders;

import static com.vibrent.drc.constants.KafkaConstants.*;

public class MessageHeadersUtil {

    private MessageHeadersUtil() {
        //Private Constructor
    }

    public static MessageHeaderDto buildMessageHeaderDto(MessageHeaders messageHeaders) {
        String originator = (String) messageHeaders.get(KAFKA_HEADER_ORIGINATOR);
        String pattern = (String) messageHeaders.get(KAFKA_HEADER_PATTERN);
        String messageSpec = (String) messageHeaders.get(KAFKA_HEADER_MESSAGE_SPEC);
        String trigger = (String) messageHeaders.get(KAFKA_HEADER_TRIGGER);
        String workflowName = (String) messageHeaders.get(KAFKA_HEADER_WORKFLOWNAME);
        Object messageTimestamp = messageHeaders.get(KAFKA_HEADER_MESSAGE_TIMESTAMP);
        Object tenantId = messageHeaders.get(KAFKA_HEADER_TENANT_ID);
        Object programId = messageHeaders.get(KAFKA_HEADER_PROGRAM_ID);


        MessageHeaderDto vxpRequestHeader = new MessageHeaderDto();
        vxpRequestHeader.setVxpHeaderVersion((String) messageHeaders.get(KAFKA_HEADER_VERSION));
        vxpRequestHeader.setVxpOriginator(originator != null ? RequestOriginatorEnum.valueOf(originator) : null);
        vxpRequestHeader.setVxpPattern(pattern != null ? IntegrationPatternEnum.valueOf(pattern) : null);
        vxpRequestHeader.setVxpMessageSpec(messageSpec != null ? MessageSpecificationEnum.valueOf(messageSpec) : null);
        vxpRequestHeader.setVxpMessageSpecVersion((String) messageHeaders.get(KAFKA_HEADER_MESSAGE_SPEC_VERSION));
        vxpRequestHeader.setVxpTenantID(tenantId != null ? Long.parseLong(tenantId.toString()) : null);
        vxpRequestHeader.setVxpProgramID(programId != null ? Long.parseLong(programId.toString()) : null);
        vxpRequestHeader.setVxpTrigger(trigger != null ? ContextTypeEnum.valueOf(trigger) : null);
        vxpRequestHeader.setVxpWorkflowName(workflowName != null ? WorkflowNameEnum.valueOf(workflowName) : null);
        vxpRequestHeader.setVxpWorkflowInstanceID((String) messageHeaders.get(KAFKA_HEADER_WORKFLOW_INSTANCE_ID));
        vxpRequestHeader.setVxpMessageID((String) messageHeaders.get(KAFKA_HEADER_MESSAGE_ID));
        vxpRequestHeader.setVxpMessageTimestamp(messageTimestamp != null ? Long.parseLong(messageTimestamp.toString()) : System.currentTimeMillis());
        vxpRequestHeader.setVxpInReplyToID((String) messageHeaders.get(KAFKA_HEADER_REPLY_TO_ID));
        return vxpRequestHeader;
    }
}
