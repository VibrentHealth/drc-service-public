package com.vibrent.drc.messaging.producer;

import com.vibrent.drc.constants.KafkaConstants;
import com.vibrent.vxp.push.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@Slf4j
@Service
public class DrcExternalEventProducer implements MessageProducer<DRCExternalEventDto> {
    private final String topicName;
    private boolean kafkaEnabled;
    private final KafkaTemplate<String, DRCExternalEventDto> drcExternalEventDtoKafkaTemplate;

    public DrcExternalEventProducer(@Value("${spring.kafka.topics.eventDRCNotificationRequest}") String topicName,
                                    @Value("${spring.kafka.enabled}") boolean kafkaEnabled, KafkaTemplate<String, DRCExternalEventDto> drcExternalEventDtoKafkaTemplate) {
        this.topicName = topicName;
        this.kafkaEnabled = kafkaEnabled;
        this.drcExternalEventDtoKafkaTemplate = drcExternalEventDtoKafkaTemplate;
    }

    @Override
    public void setKafkaEnabled(boolean newState) {
        this.kafkaEnabled = newState;
    }

    @Override
    public void send(DRCExternalEventDto msg) {
        if (!kafkaEnabled) {
            return;
        }
        if (msg == null) {
            log.warn("DRC-Service: Cannot publish external event as event dto is null");
            return;
        }
        MessageHeaderDto messageHeaderDto = buildMessageHeaderDto(msg.getVibrentID());
        Message<DRCExternalEventDto> message = KafkaMessageBuilder.buildMessage(msg, messageHeaderDto, topicName);
        drcExternalEventDtoKafkaTemplate.send(message)
                .addCallback(new ListenableFutureCallback<SendResult<String, DRCExternalEventDto>>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        log.warn("DRC-Service: Failed to send the drc external event for participant id:{}, user id:{}, event type:{}, payload:{}, error:{}", msg.getExternalID(), msg.getVibrentID(), msg.getEventType(), msg.getBody(), throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(SendResult<String, DRCExternalEventDto> stringDRCExternalEventDtoSendResult) {
                        log.info("DRC-Service: Successfully sent the drc external event for participant id:{}, user id:{}, event type:{}, payload:{}", msg.getExternalID(), msg.getVibrentID(), msg.getEventType(), msg.getBody());
                    }
                });

    }

    public static MessageHeaderDto buildMessageHeaderDto(Long userId) {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpHeaderVersion(KafkaConstants.VXP_HEADER_VERSION);
        messageHeaderDto.setVxpMessageSpec(MessageSpecificationEnum.EXTERNAL_EVENT);
        messageHeaderDto.setVxpUserID(userId);
        messageHeaderDto.setVxpProgramID(null);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.PUSH);
        messageHeaderDto.setVxpMessageSpecVersion(KafkaConstants.VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        return messageHeaderDto;
    }
}
