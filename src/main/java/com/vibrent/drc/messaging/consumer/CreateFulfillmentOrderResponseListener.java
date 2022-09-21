package com.vibrent.drc.messaging.consumer;

import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.vxp.workflow.FulfillmentResponseDto;
import com.vibrent.vxp.workflow.FulfillmentResponseDtoWrapper;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "vibrent.drc.supplyStatus.enabled", havingValue = "true")
public class CreateFulfillmentOrderResponseListener {

    private FulfillmentService fulfillmentService;

    @KafkaListener(topics = "${spring.kafka.topics.fulfillment.response}", id = "fulfillmentOrderResponseListener", containerFactory = "kafkaListenerContainerFactoryFulfillmentOrderResponseListener")
    public void listen(@Payload byte[] payloadByteArray,
                       @Headers MessageHeaders messageHeaders) {
        MessageHeaderDto messageHeaderDto = MessageHeadersUtil.buildMessageHeaderDto(messageHeaders);
        FulfillmentResponseDto fulfillmentResponseDto = null;
        try {
            fulfillmentResponseDto = convertPayloadToFulfillmentResponseDto(payloadByteArray, messageHeaderDto);
            boolean isValid = validateFulfillmentOrderStatus(fulfillmentResponseDto);
            if (isValid) {
                log.warn("DRC Service: Relevant status received in fulfillmentResponseDto: {}", fulfillmentResponseDto);
                processFulfillmentResponse(fulfillmentResponseDto);
            } else {
                log.warn("DRC Service: Relevant status not received in fulfillmentResponseDto: {}", fulfillmentResponseDto);
            }
        } catch (Exception e) {
            log.warn("DRC Service: Error while processing fulfillmentResponseDto: {}", fulfillmentResponseDto, e);
        }
    }

    private void processFulfillmentResponse(FulfillmentResponseDto fulfillmentResponseDto) throws Exception {

        //call Fulfillment Service for order details
        if (fulfillmentResponseDto.getOrder() != null && fulfillmentResponseDto.getOrder().getFulfillmentOrderID() != null) {
            var orderDetailsDto = this.fulfillmentService.getOrderById(fulfillmentResponseDto.getOrder().getFulfillmentOrderID());
            if (orderDetailsDto == null) {
                throw new BusinessProcessingException("Failed to fetch order details for orderId:- " + fulfillmentResponseDto.getOrder().getFulfillmentOrderID());
            }
            log.info("DRC Service: Order details from Fulfillment service: {}", orderDetailsDto.toString());
        }
    }

    private boolean validateFulfillmentOrderStatus(FulfillmentResponseDto fulfillmentResponseDto) {
        if (fulfillmentResponseDto == null || fulfillmentResponseDto.getStatus() == null) {
            return false;
        }
        OrderStatusEnum status = fulfillmentResponseDto.getStatus();

        return (OrderStatusEnum.CREATED.equals(status) || OrderStatusEnum.PENDING_SHIPMENT.equals(status) ||
                OrderStatusEnum.SHIPPED.equals(status) || OrderStatusEnum.PARTICIPANT_IN_TRANSIT.equals(status) ||
                OrderStatusEnum.PARTICIPANT_DELIVERED.equals(status) || OrderStatusEnum.RETURN_IN_TRANSIT.equals(status) ||
                OrderStatusEnum.RETURN_DELIVERED.equals(status) || OrderStatusEnum.ERROR.equals(status));
    }

    private FulfillmentResponseDto convertPayloadToFulfillmentResponseDto(byte[] payloadByteArray, MessageHeaderDto messageHeaderDto) {
        FulfillmentResponseDtoWrapper fulfillmentResponseDtoWrapper = new FulfillmentResponseDtoWrapper(payloadByteArray, messageHeaderDto);
        FulfillmentResponseDto fulfillmentResponseDto = null;
        try {
            fulfillmentResponseDto = fulfillmentResponseDtoWrapper.getPayload();
        } catch (IOException e) {
            log.warn("DRC Service: Cannot convert Payload to FulfillmentResponseDto", e);
        }
        return fulfillmentResponseDto;
    }
}
