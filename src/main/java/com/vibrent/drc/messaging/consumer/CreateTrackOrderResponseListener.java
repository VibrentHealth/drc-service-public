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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "vibrent.drc.supplyStatus.enabled", havingValue = "true")
public class CreateTrackOrderResponseListener {

    private final DRCSalivaryOrderService drcSalivaryOrderService;
    private final FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;
    private final String topicName;
    private final ExternalApiRequestLogsService externalApiRequestLogsService;
    private final OrderTrackingDetailsService orderTrackingDetailsService;

    @Inject
    public CreateTrackOrderResponseListener(DRCSalivaryOrderService drcSalivaryOrderService,
                                            FHIRSalivaryConverterUtility fhirSalivaryConverterUtility,
                                            @Value("${spring.kafka.topics.vxpResponse}") String topicName,
                                            ExternalApiRequestLogsService externalApiRequestLogsService,
                                            OrderTrackingDetailsService orderTrackingDetailsService) {
        this.drcSalivaryOrderService = drcSalivaryOrderService;
        this.fhirSalivaryConverterUtility = fhirSalivaryConverterUtility;
        this.topicName = topicName;
        this.externalApiRequestLogsService = externalApiRequestLogsService;
        this.orderTrackingDetailsService = orderTrackingDetailsService;
    }

    @KafkaListener(topics = "${spring.kafka.topics.vxpResponse}", id = "drcCreateTrackOrderResponseListener", containerFactory = "kafkaListenerContainerFactoryTrackOrderResponseListener")
    public void listen(@Payload byte[] payloadByteArray,
                       @Headers MessageHeaders messageHeaders) {
        MessageHeaderDto messageHeaderDto = MessageHeadersUtil.buildMessageHeaderDto(messageHeaders);
        CreateTrackOrderResponseDto createTrackOrderResponseDto = convertPayloadToCreateTrackOrderDto(payloadByteArray, messageHeaderDto);
        try {
            boolean isValid = validateCreateTrackOrderStatus(createTrackOrderResponseDto);
            if (isValid) {
                processCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
            }
        } catch (Exception e) {
            log.warn("DRC Service: Error while processing CreateTrackOrderResponse: {}", createTrackOrderResponseDto, e);
        }
    }

    private boolean validateCreateTrackOrderStatus(CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        if (createTrackOrderResponseDto == null || createTrackOrderResponseDto.getStatus() == null) {
            return false;
        }
        StatusEnum statusEnum = createTrackOrderResponseDto.getStatus();
        String orderId = getOrderId(createTrackOrderResponseDto);
        if (StringUtils.isEmpty(orderId)) {
            log.warn("DRC Service: ORDER_ID identifier received as null in createTrackOrderResponseDto: {}", createTrackOrderResponseDto);
            return false;
        }

        OrderTrackingDetails orderTrackingDetails = this.orderTrackingDetailsService.getOrderDetails(orderId);
        return OrderStatusUtil.isStatusAfter(statusEnum, orderTrackingDetails);
    }

    private String getOrderId(CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        List<IdentifierDto> identifierDtoList = createTrackOrderResponseDto.getIdentifiers();
        if (identifierDtoList == null || identifierDtoList.isEmpty()) {
            return null;
        }

        for (IdentifierDto identifierDto : identifierDtoList) {
            if(identifierDto.getType() == IdentifierTypeEnum.ORDER_ID) {
                return identifierDto.getId();
            }
        }
        return null;
    }

    private CreateTrackOrderResponseDto convertPayloadToCreateTrackOrderDto(byte[] payloadByteArray, MessageHeaderDto messageHeaderDto) {
        CreateTrackOrderResponseDtoWrapper createTrackOrderResponseDtoWrapper = new CreateTrackOrderResponseDtoWrapper(payloadByteArray, messageHeaderDto);
        CreateTrackOrderResponseDto createTrackOrderResponseDto = null;
        try {
            createTrackOrderResponseDto = createTrackOrderResponseDtoWrapper.getPayload();
        } catch (IOException e) {
            log.warn("DRC Service: Cannot convert Payload to CreateTrackOrderResponseDto", e);
        }
        return createTrackOrderResponseDto;
    }

    private void processCreateTrackOrderResponse(CreateTrackOrderResponseDto createTrackOrderResponseDto, com.vibrent.vxp.workflow.MessageHeaderDto messageHeaderDto) {
        if (createTrackOrderResponseDto == null) {
            return;
        }

        try {
            ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLogForResponseReceived(messageHeaderDto, createTrackOrderResponseDto, topicName, "DRC Service received message from VXP order service", 200);
            externalApiRequestLogsService.send(externalApiRequestLog);

            this.fhirSalivaryConverterUtility.setParticipantAddress(createTrackOrderResponseDto.getParticipant());
            if (!logCreateTrackOrderErrors(createTrackOrderResponseDto)) {
                this.drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
            }
        } catch (BusinessValidationException e) {
            log.warn("DRC Service: Error while processing Create Track Order Response. Error Message: {}", e.getMessage(), e);
        }
    }

    private boolean logCreateTrackOrderErrors(CreateTrackOrderResponseDto createTrackOrderResponseDto) {
        if (createTrackOrderResponseDto.getErrors() == null) {
            return false;
        }
        for (ErrorPayloadDto error : createTrackOrderResponseDto.getErrors()) {
            if (error.getCode() != null && ErrorCodeEnum.GENOTEK_TECHNICAL_ERROR.equals(error.getCode()) &&
                    createTrackOrderResponseDto.getProvider() != null && ProviderEnum.GENOTEK.equals(createTrackOrderResponseDto.getProvider())) {
                log.error("DRC Service: Genotek Technical error response received: {}" , createTrackOrderResponseDto);
                return true;
            }
        }
        return false;
    }
}
