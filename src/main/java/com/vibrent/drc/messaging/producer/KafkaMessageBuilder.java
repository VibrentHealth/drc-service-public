package com.vibrent.drc.messaging.producer;


import com.vibrent.drc.constants.KafkaConstants;
import com.vibrent.vxp.push.MessageHeaderDto;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class KafkaMessageBuilder {
    private KafkaMessageBuilder() {
    }

    public static <T> Message<T> buildMessage(T payload, MessageHeaderDto messageHeaderDto, String topicName) {

        String vxpMessageSpec = messageHeaderDto.getVxpMessageSpec() != null ? messageHeaderDto.getVxpMessageSpec().toValue() : null;
        String vxpOriginator = messageHeaderDto.getVxpOriginator() != null ? messageHeaderDto.getVxpOriginator().toValue() : null;
        String vxpPattern = messageHeaderDto.getVxpPattern() != null ? messageHeaderDto.getVxpPattern().toValue() : null;
        String vxpTrigger = messageHeaderDto.getVxpTrigger() != null ? messageHeaderDto.getVxpTrigger().toValue() : null;
        String vxpWorkflowName = messageHeaderDto.getVxpWorkflowName() != null ? messageHeaderDto.getVxpWorkflowName().toValue() : null;

        MessageBuilder<T> messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, topicName);
        messageBuilder.setHeader(KafkaHeaders.MESSAGE_KEY, messageHeaderDto.getVxpMessageID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC, vxpMessageSpec);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_VERSION, messageHeaderDto.getVxpHeaderVersion());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_ORIGINATOR, vxpOriginator);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_PATTERN, vxpPattern);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC, vxpMessageSpec);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC_VERSION, messageHeaderDto.getVxpMessageSpecVersion());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_TENANT_ID, messageHeaderDto.getVxpTenantID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_PROGRAM_ID, messageHeaderDto.getVxpProgramID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_TRIGGER, vxpTrigger);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_WORKFLOWNAME, vxpWorkflowName);
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_WORKFLOW_INSTANCE_ID, messageHeaderDto.getVxpWorkflowInstanceID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_MESSAGE_ID, messageHeaderDto.getVxpMessageID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_REPLY_TO_ID, messageHeaderDto.getVxpInReplyToID());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_MESSAGE_TIMESTAMP, messageHeaderDto.getVxpMessageTimestamp());
        messageBuilder.setHeader(KafkaConstants.KAFKA_HEADER_USER_ID, messageHeaderDto.getVxpUserID());
        return messageBuilder.build();
    }

}
