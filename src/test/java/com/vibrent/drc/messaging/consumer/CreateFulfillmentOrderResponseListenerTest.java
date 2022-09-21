package com.vibrent.drc.messaging.consumer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.fulfillment.dto.ProductDTO;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vibrent.drc.constants.KafkaConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFulfillmentOrderResponseListenerTest {

    private final Long vibrentID = 132435L;
    private final Long programID = 132434L;

    @Mock
    FulfillmentService fulfillmentService;

    private CreateFulfillmentOrderResponseListener createFulfillmentOrderResponseListener;

    @BeforeEach
    void setUp() {
        createFulfillmentOrderResponseListener = new CreateFulfillmentOrderResponseListener(fulfillmentService);
    }

    @Test
    @DisplayName("when fulfillment response received with created status and valid fulfillmentId " +
            "then process fulfillment response and fetch order info")
    void testCreateFulfillmentOrderResponseListener() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(100L,"ORDER_TYPE","P_CODE",2L));
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(buildOrderDetailsDTO());

        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        assertEquals("INFO", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("Order details from Fulfillment service"));
    }

    @Test
    @DisplayName("when fulfillment response received with created status and valid fulfillmentId but null orderDetails found" +
            "then throw businessProcessing exception and handled in listener ")
    void testCreateFulfillmentOrderResponseListenerWithNullOrderDetailsDto() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(100L,"ORDER_TYPE","P_CODE",2L));
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(null);

        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("Error while processing fulfillmentResponseDto"));
    }

    @Test
    @DisplayName("when fulfillment response received with created status and null fulfillmentId" +
            "then don't process Fulfillment response")
    void testCreateFulfillmentOrderResponseListenerWithNullFulfillmentId() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(null,"ORDER_TYPE","P_CODE",2L));
        Mockito.verify(fulfillmentService, times(0)).getOrderById(anyLong());
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.PARTICIPANT_AVAILABLE_TO_PICKUP);
        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status not received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        Mockito.verify(fulfillmentService, times(0)).getOrderById(anyLong());
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerException() {
        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(null, buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Error while processing fulfillmentResponseDto: {}",logsList.get(0).getMessage());
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerNullStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(null);
        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status not received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerErrorStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.ERROR);
        Logger logger = (Logger) LoggerFactory.getLogger(CreateFulfillmentOrderResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        createFulfillmentOrderResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        Mockito.verify(fulfillmentService, times(0)).getOrderById(anyLong());
    }

    private byte[] buildPayload(FulfillmentResponseDto fulfillmentResponseDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(fulfillmentResponseDto);
    }

    private FulfillmentResponseDto createFulfillmentResponseDto() {
        FulfillmentResponseDto fulfillmentResponseDto = new FulfillmentResponseDto();


        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);
        fulfillmentResponseDto.setProgramID(programID);
        fulfillmentResponseDto.setVibrentID(vibrentID);
        return fulfillmentResponseDto;
    }

    private MessageHeaders buildMessageHeaders(){
        Map<String, Object> headers = new HashMap<>();
        MessageHeaders messageHeaders;
        headers.put(KAFKA_HEADER_MESSAGE_SPEC, MessageSpecificationEnum.FULFILLMENT_RESPONSE.toValue());
        headers.put(KAFKA_HEADER_REPLY_TO_ID, "inReplyTo");
        headers.put(KAFKA_HEADER_MESSAGE_ID, "messageId");
        headers.put(KAFKA_HEADER_MESSAGE_SPEC_VERSION, "version");
        headers.put(KAFKA_HEADER_MESSAGE_TIMESTAMP, 12345678L);
        headers.put(KAFKA_HEADER_PATTERN, IntegrationPatternEnum.WORKFLOW.toValue());
        headers.put(KAFKA_HEADER_PROGRAM_ID, 2L);
        headers.put(KAFKA_HEADER_TENANT_ID, 1L);
        headers.put(KAFKA_HEADER_TRIGGER, ContextTypeEnum.EVENT.toValue());
        headers.put(KAFKA_HEADER_WORKFLOW_INSTANCE_ID, "instanceID");
        headers.put(KAFKA_HEADER_VERSION, "2.1.4");
        headers.put(KAFKA_HEADER_WORKFLOWNAME, WorkflowNameEnum.SALIVARY_KIT_ORDER.toValue());
        headers.put(KAFKA_HEADER_ORIGINATOR, RequestOriginatorEnum.VXPMS.toValue());

        messageHeaders = new MessageHeaders(headers);
        return messageHeaders;
    }

    private OrderDetailsDTO buildOrderDetailsDTO() {

        OrderDetailsDTO orderDetailsDTO = new OrderDetailsDTO();
        orderDetailsDTO.setId(100L);
        orderDetailsDTO.setProduct(new ProductDTO());
        orderDetailsDTO.setQuantity(1L);
        orderDetailsDTO.setStatus("SHIPPED");
        orderDetailsDTO.setProgramId(106L);
        return orderDetailsDTO;
    }

}