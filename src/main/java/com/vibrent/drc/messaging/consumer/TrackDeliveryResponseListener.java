package com.vibrent.drc.messaging.consumer;

import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.service.OrderTrackingDetailsService;
import com.vibrent.drc.service.impl.FHIRSalivaryConverterUtility;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrent.drc.util.OrderStatusUtil;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@ConditionalOnProperty(name = "vibrent.drc.supplyStatus.enabled", havingValue = "true")
@Component
@Slf4j
public class TrackDeliveryResponseListener {

    private final DRCSalivaryOrderService drcSalivaryOrderService;
    private final FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;
    private final String topicName;
    private final ExternalApiRequestLogsService externalApiRequestLogsService;
    private final OrderTrackingDetailsService orderTrackingDetailsService;

    public TrackDeliveryResponseListener(DRCSalivaryOrderService drcSalivaryOrderService, FHIRSalivaryConverterUtility fhirSalivaryConverterUtility,
                                         @Value("${spring.kafka.topics.vxpResponse}") String topicName, ExternalApiRequestLogsService externalApiRequestLogsService,
                                         OrderTrackingDetailsService orderTrackingDetailsService) {
        this.drcSalivaryOrderService = drcSalivaryOrderService;
        this.fhirSalivaryConverterUtility = fhirSalivaryConverterUtility;
        this.topicName = topicName;
        this.externalApiRequestLogsService = externalApiRequestLogsService;
        this.orderTrackingDetailsService = orderTrackingDetailsService;
    }

    @KafkaListener(topics = "${spring.kafka.topics.vxpResponse}", id = "drcTrackDeliveryResponseGroupId", containerFactory = "kafkaListenerContainerFactoryTrackDeliveryResponseEvent")
    public void listen(@Payload byte[] payloadByteArray,
                       @Headers MessageHeaders messageHeaders) {
        MessageHeaderDto messageHeaderDto = MessageHeadersUtil.buildMessageHeaderDto(messageHeaders);
        TrackDeliveryResponseDto trackDeliveryResponseDto = convertPayloadToTrackDeliveryResponseDto(payloadByteArray, messageHeaderDto);
        try {
            boolean isValid = validateTrackDeliveryStatus(trackDeliveryResponseDto);
            if (isValid) {
                processTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
            }
        } catch (Exception e) {
            log.warn("DRC Service: Error while processing trackDeliveryResponseDto: {}", trackDeliveryResponseDto, e);
        }
    }

    private boolean validateTrackDeliveryStatus(TrackDeliveryResponseDto trackDeliveryResponseDto) {
        if (trackDeliveryResponseDto == null || trackDeliveryResponseDto.getStatus() == null) {
            return false;
        }
        StatusEnum statusEnum = trackDeliveryResponseDto.getStatus();
        String trackingID = trackDeliveryResponseDto.getTrackingID();
        OrderTrackingDetails orderTrackingDetails = this.orderTrackingDetailsService.getOrderDetails(trackingID);
        return OrderStatusUtil.isStatusAfter(statusEnum, orderTrackingDetails);
    }

    private TrackDeliveryResponseDto convertPayloadToTrackDeliveryResponseDto(byte[] payloadByteArray, MessageHeaderDto messageHeaderDto) {
        TrackDeliveryResponseDtoWrapper trackDeliveryResponseDtoWrapper = new TrackDeliveryResponseDtoWrapper(payloadByteArray, messageHeaderDto);
        TrackDeliveryResponseDto trackDeliveryResponseDto = null;
        try {
            trackDeliveryResponseDto = trackDeliveryResponseDtoWrapper.getPayload();
        } catch (IOException e) {
            log.warn("DRC Service: Cannot convert Payload to trackDeliveryResponseDto", e);
        }
        return trackDeliveryResponseDto;
    }

    private void processTrackDeliveryResponse(TrackDeliveryResponseDto trackDeliveryResponseDto, MessageHeaderDto messageHeaderDto) {
        if (trackDeliveryResponseDto == null) {
            return;
        }
        try {
            ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLogForResponseReceived(messageHeaderDto, trackDeliveryResponseDto, topicName, "DRC Service received message from VXP tracking service", 200);
            externalApiRequestLogsService.send(externalApiRequestLog);
            this.fhirSalivaryConverterUtility.setParticipantAddress(trackDeliveryResponseDto.getParticipant());
            if (!logTrackDeliveryResponseErrors(trackDeliveryResponseDto)) {
                this.drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
            }
        } catch (BusinessValidationException e) {
            log.warn("API: Error while processing Track Delivery Response.", e);
        }
    }

    private boolean logTrackDeliveryResponseErrors(TrackDeliveryResponseDto trackDeliveryResponseDto) {
        if (trackDeliveryResponseDto.getErrors() == null) {
            return false;
        }
        for (ErrorPayloadDto error : trackDeliveryResponseDto.getErrors()) {
            if (error.getCode() != null && trackDeliveryResponseDto.getProvider() != null && ProviderEnum.USPS.equals(trackDeliveryResponseDto.getProvider())) {
                log.error("VXPListener: USPS error response received: {}" , trackDeliveryResponseDto);
                return true;
            }
        }
        return false;
    }

}
