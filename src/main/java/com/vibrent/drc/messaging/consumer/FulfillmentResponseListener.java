package com.vibrent.drc.messaging.consumer;

import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.drc.converter.AddressConverter;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.vxp.workflow.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vibrent.drc.supplyStatus.enabled", havingValue = "true")
public class FulfillmentResponseListener {

    private final FulfillmentService fulfillmentService;
    private final ExternalApiRequestLogsService externalApiRequestLogsService;
    private final DRCSalivaryOrderService drcSalivaryOrderService;
    private final AddressConverter addressConverter;
    private final ApiService apiService;

    @Value("${spring.kafka.topics.fulfillment.response}")
    private String topicName;

    @KafkaListener(topics = "${spring.kafka.topics.fulfillment.response}", id = "fulfillmentOrderResponseListener", containerFactory = "kafkaListenerContainerFactoryFulfillmentOrderResponseListener")
    public void listen(@Payload byte[] payloadByteArray,
                       @Headers MessageHeaders messageHeaders) {
        MessageHeaderDto messageHeaderDto = MessageHeadersUtil.buildMessageHeaderDto(messageHeaders);
        FulfillmentResponseDto fulfillmentResponseDto = null;
        try {
            fulfillmentResponseDto = convertPayloadToFulfillmentResponseDto(payloadByteArray, messageHeaderDto);
            boolean isValid = validateFulfillmentOrderStatus(fulfillmentResponseDto);
            if (isValid) {
                log.info("DRC Service: Relevant status received in fulfillmentResponseDto: {}", fulfillmentResponseDto);
                processFulfillmentResponse(fulfillmentResponseDto, messageHeaderDto);
            } else {
                log.info("DRC Service: Relevant status not received in fulfillmentResponseDto: {}", fulfillmentResponseDto);
            }
        } catch (Exception e) {
            log.warn("DRC Service: Error while processing fulfillmentResponseDto: {}", fulfillmentResponseDto, e);
        }
    }

    private void processFulfillmentResponse(FulfillmentResponseDto fulfillmentResponseDto, MessageHeaderDto messageHeaderDto) throws DrcException {

        //call Fulfillment Service for order details
        OrderDetailsDTO orderDetailsDTO = fetchOrderDetails(fulfillmentResponseDto);
        //fetch participant using userId and participantId
        ParticipantDto participantDto = getParticipantDto(fulfillmentResponseDto.getVibrentID(), orderDetailsDTO);

        try {
            ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLogForResponseReceived(messageHeaderDto, fulfillmentResponseDto, topicName, "DRC Service received message from VXP order service", 200, participantDto);
            externalApiRequestLogsService.send(externalApiRequestLog);

            this.drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, messageHeaderDto, orderDetailsDTO, participantDto);
        } catch (BusinessValidationException e) {
            log.warn("DRC Service: Error while processing Create Fulfillment Order Response. Error Message: {}", e.getMessage(), e);
        }
    }

    private ParticipantDto getParticipantDto(long vibrentID, OrderDetailsDTO orderDetailsDTO) {
        ParticipantDto participantDto = new ParticipantDto();

        UserDTO userDTO = this.apiService.getUserDTO(vibrentID);
        if (orderDetailsDTO.getAddress() != null) {
            setParticipantAddress(orderDetailsDTO, participantDto);
        } else if (userDTO!= null && userDTO.getMailingAddress() != null) {
            setParticipantAddressFromUserDto(participantDto, userDTO.getMailingAddress());
        } else {
            log.warn("DRC Service: User does not have mailing address. Participant ID: {}", vibrentID);
        }

        participantDto.setVibrentID(vibrentID);
        if (userDTO!= null && userDTO.getExternalId() != null) {
            participantDto.setExternalID(userDTO.getExternalId());
        }

        return participantDto;
    }

    private void setParticipantAddressFromUserDto(ParticipantDto participantDto, UserAddressDTO userAddressDTO) {
        AddressDto addressDto = new AddressDto();
        addressDto.setLine1(userAddressDTO.getStreetOne());
        addressDto.setLine2(userAddressDTO.getStreetTwo());
        addressDto.setCity(userAddressDTO.getCity());
        addressDto.setPostalCode(userAddressDTO.getZip());
        addressDto.setState(userAddressDTO.getState());
        List<AddressDto> singleAddressList = new ArrayList<>();
        singleAddressList.add(addressDto);
        participantDto.setAddresses(singleAddressList);
    }

    private void setParticipantAddress(OrderDetailsDTO orderDetailsDTO, ParticipantDto participantDto) {
        List<AddressDto> singleAddressList = new ArrayList<>();
        AddressDto workflowAddressDto = new AddressDto();
        addressConverter.convertToAddressDto(workflowAddressDto, orderDetailsDTO.getAddress());
        singleAddressList.add(workflowAddressDto);
        participantDto.setAddresses(singleAddressList);
    }

    private OrderDetailsDTO fetchOrderDetails(FulfillmentResponseDto fulfillmentResponseDto) throws DrcException {
        OrderDetailsDTO orderDetailsDto = null;
        if (fulfillmentResponseDto.getOrder() == null || fulfillmentResponseDto.getOrder().getFulfillmentOrderID() == null) {
            throw new BusinessProcessingException("Order details are missing in Fulfillment response");
        }

        orderDetailsDto = this.fulfillmentService.getOrderById(fulfillmentResponseDto.getOrder().getFulfillmentOrderID());
        if (orderDetailsDto == null) {
            throw new BusinessProcessingException("Failed to fetch order details for orderId:- " + fulfillmentResponseDto.getOrder().getFulfillmentOrderID());
        } else if (ObjectUtils.isEmpty(orderDetailsDto.getSupplierOrderId())) {
            throw new BusinessProcessingException("Supplier orderId is missing for orderId:- " + fulfillmentResponseDto.getOrder().getFulfillmentOrderID());
        }
        log.info("DRC Service: Order details from Fulfillment service: {}", orderDetailsDto.toString());
        return orderDetailsDto;
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
