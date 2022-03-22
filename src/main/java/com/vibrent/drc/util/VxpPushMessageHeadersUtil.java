package com.vibrent.drc.util;

import com.vibrent.vxp.push.MessageHeaderDto;
import org.springframework.messaging.MessageHeaders;

import static com.vibrent.drc.constants.KafkaConstants.*;

public class VxpPushMessageHeadersUtil {

    private VxpPushMessageHeadersUtil() {
        //Private Constructor
    }

    public static MessageHeaderDto buildVxpPushMessageHeaderDto(MessageHeaders messageHeaders) {
        String originator = (String) messageHeaders.get(KAFKA_HEADER_ORIGINATOR);
        String pattern = (String) messageHeaders.get(KAFKA_HEADER_PATTERN);
        String messageSpec = (String) messageHeaders.get(KAFKA_HEADER_MESSAGE_SPEC);
        String trigger = (String) messageHeaders.get(KAFKA_HEADER_TRIGGER);
        String workflowName = (String) messageHeaders.get(KAFKA_HEADER_WORKFLOWNAME);
        Object messageTimestamp = messageHeaders.get(KAFKA_HEADER_MESSAGE_TIMESTAMP);
        Object tenantId = messageHeaders.get(KAFKA_HEADER_TENANT_ID);
        Object programId = messageHeaders.get(KAFKA_HEADER_PROGRAM_ID);


        MessageHeaderDto vxpRequestHeader = new com.vibrent.vxp.push.MessageHeaderDto();
        vxpRequestHeader.setVxpHeaderVersion((String) messageHeaders.get(KAFKA_HEADER_VERSION));
        vxpRequestHeader.setVxpOriginator(originator != null ? com.vibrent.vxp.push.RequestOriginatorEnum.valueOf(originator) : null);
        vxpRequestHeader.setVxpPattern(pattern != null ? com.vibrent.vxp.push.IntegrationPatternEnum.valueOf(pattern) : null);
        vxpRequestHeader.setVxpMessageSpec(messageSpec != null ? com.vibrent.vxp.push.MessageSpecificationEnum.valueOf(messageSpec) : null);
        vxpRequestHeader.setVxpMessageSpecVersion((String) messageHeaders.get(KAFKA_HEADER_MESSAGE_SPEC_VERSION));
        vxpRequestHeader.setVxpTenantID(tenantId != null ? Long.parseLong(tenantId.toString()) : null);
        vxpRequestHeader.setVxpProgramID(programId != null ? Long.parseLong(programId.toString()) : null);
        vxpRequestHeader.setVxpTrigger(trigger != null ? com.vibrent.vxp.push.ContextTypeEnum.valueOf(trigger) : null);
        vxpRequestHeader.setVxpWorkflowName(workflowName != null ? com.vibrent.vxp.push.WorkflowNameEnum.valueOf(workflowName) : null);
        vxpRequestHeader.setVxpWorkflowInstanceID((String) messageHeaders.get(KAFKA_HEADER_WORKFLOW_INSTANCE_ID));
        vxpRequestHeader.setVxpMessageID((String) messageHeaders.get(KAFKA_HEADER_MESSAGE_ID));
        vxpRequestHeader.setVxpMessageTimestamp(messageTimestamp != null ? Long.parseLong(messageTimestamp.toString()) : System.currentTimeMillis());
        vxpRequestHeader.setVxpInReplyToID((String) messageHeaders.get(KAFKA_HEADER_REPLY_TO_ID));
        return vxpRequestHeader;
    }
}
