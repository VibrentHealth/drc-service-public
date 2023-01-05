package com.vibrent.drc.messaging.consumer;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.service.DRCParticipantService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.VxpPushMessageHeadersUtil;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import com.vibrent.vxp.push.MessageHeaderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;

@Slf4j
@Component
@ConditionalOnProperty(name = "vibrent.drc.accountInfoUpdates.enabled", havingValue = "true")
public class AccountInfoUpdateEventListener {

    private final AccountInfoUpdateEventService accountInfoUpdateEventService;
    private final ExternalApiRequestLogsService externalApiRequestLogsService;
    private final String topicName;
    private final DRCParticipantService participantService;

    @Inject
    public AccountInfoUpdateEventListener(AccountInfoUpdateEventService accountInfoUpdateEventService,
                                          ExternalApiRequestLogsService externalApiRequestLogsService,
                                          @Value("${spring.kafka.topics.pushParticipant}") String topicName,
                                          DRCParticipantService participantService) {
        this.accountInfoUpdateEventService = accountInfoUpdateEventService;
        this.externalApiRequestLogsService = externalApiRequestLogsService;
        this.topicName = topicName;
        this.participantService = participantService;
    }

    @KafkaListener(topics = "${spring.kafka.topics.pushParticipant}", id = "drcAccountInfoUpdateEventListener", containerFactory = "kafkaListenerContainerFactoryAccountInfoUpdateEventListener")
    public void listen(@Payload byte[] payloadByteArray,
                       @Headers MessageHeaders messageHeaders) {

        AccountInfoUpdateEventDto accountInfoUpdateEventDto = convertToAccountInfoUpdateEventDto(payloadByteArray, messageHeaders);

        if (accountInfoUpdateEventDto == null) {
            return;
        }

        if (accountInfoUpdateEventDto.getParticipant() == null) {
            log.warn("drc-service: Discarding accountInfoUpdateEvent as participant info is null ");
            return;
        }

        try {
            MessageHeaderDto messageHeaderDto = VxpPushMessageHeadersUtil.buildVxpPushMessageHeaderDto(messageHeaders);
            ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLogForAccountInfoReceived(
                    messageHeaderDto, accountInfoUpdateEventDto, topicName, "DRC Service received Account Info Update event from VRP", 200);
            this.externalApiRequestLogsService.send(externalApiRequestLog);

            //Process accountInfoUpdateEvent
            this.accountInfoUpdateEventService.processAccountInfoUpdates(accountInfoUpdateEventDto);

            //Patch participant
            this.participantService.patchTestParticipant(accountInfoUpdateEventDto);
        } catch (Exception e) {
            log.error("DRC: Error while processing accountInfoUpdateEventDto: {} messageHeaders: {}", accountInfoUpdateEventDto, messageHeaders, e);
        }

    }

    AccountInfoUpdateEventDto convertToAccountInfoUpdateEventDto(byte[] payloadByteArray, MessageHeaders messageHeaders) {
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = null;
        try {
            accountInfoUpdateEventDto = JacksonUtil.getMapper().readValue(payloadByteArray, AccountInfoUpdateEventDto.class);
        } catch (Exception e) {
            log.warn("drc-service: Cannot convert Payload to accountInfoUpdateEventDto  headers {} payload: {}",  messageHeaders.toString(), Arrays.toString(payloadByteArray), e);
        }
        return accountInfoUpdateEventDto;
    }
}
