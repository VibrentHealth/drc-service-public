package com.vibrent.drc.messaging.consumer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.acadia.domain.enumeration.AddressType;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.drc.converter.AddressConverter;
import com.vibrent.drc.converter.AddressConverterImpl;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.DRCSalivaryOrderService;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.fulfillment.dto.AddressDto;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.fulfillment.dto.ProductDTO;
import com.vibrent.vxp.workflow.*;
import lombok.SneakyThrows;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentResponseListenerTest {

    private final Long vibrentID = 132435L;
    private final Long programID = 132434L;

    @Mock
    FulfillmentService fulfillmentService;
    @Mock
    ExternalApiRequestLogsService externalApiRequestLogsService;
    @Mock
    DRCSalivaryOrderService drcSalivaryOrderService;
    AddressConverter addressConverter;
    @Mock
    ApiService apiService;

    private FulfillmentResponseListener fulfillmentResponseListener;

    @BeforeEach
    void setUp() {
        addressConverter = new AddressConverterImpl();
        fulfillmentResponseListener = new FulfillmentResponseListener(fulfillmentService, externalApiRequestLogsService, drcSalivaryOrderService, addressConverter,apiService);
    }

    @Test
    @DisplayName("when fulfillment response received with created status and valid fulfillmentId " +
            "then process fulfillment response and fetch order info")
    void testCreateFulfillmentOrderResponseListener() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(100L,"ORDER_TYPE","P_CODE",2L));
        var orderDetailsDTO = buildOrderDetailsDTO();
        orderDetailsDTO.setAddress(new AddressDto());
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(orderDetailsDTO);
        when(this.apiService.getUserDTO(anyLong())).thenReturn(getUserDto(vibrentID));

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        assertEquals("INFO", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("Order details from Fulfillment service"));
        verify(fulfillmentService, times(1)).getOrderById(anyLong());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    @DisplayName("when fulfillment response received with created status and valid fulfillmentId but null orderDetails found" +
            "then throw businessProcessing exception and handled in listener ")
    void testCreateFulfillmentOrderResponseListenerWithNullOrderDetailsDto() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(100L,"ORDER_TYPE","P_CODE",2L));
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(null);

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("Error while processing fulfillmentResponseDto"));
        verify(drcSalivaryOrderService, times(0)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    @SneakyThrows
    @DisplayName("when fulfillment response received with created status and null fulfillmentId" +
            "then don't process Fulfillment response")
    void testCreateFulfillmentOrderResponseListenerWithNullFulfillmentId() {

        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(new OrderDto(null,"ORDER_TYPE","P_CODE",2L));
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("DRC Service: Error while processing fulfillmentResponseDto"));
        verify(fulfillmentService, times(0)).getOrderById(anyLong());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    @SneakyThrows
    @DisplayName("when fulfillment response received with created status and null ord er" +
            "then don't process Fulfillment response")
    void testCreateFulfillmentOrderResponseListenerWithNullOrder() {

        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setOrder(null);
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertTrue(logsList.get(1).getMessage().contains("DRC Service: Error while processing fulfillmentResponseDto"));
        verify(fulfillmentService, times(0)).getOrderById(anyLong());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.PARTICIPANT_AVAILABLE_TO_PICKUP);
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status not received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        Mockito.verify(fulfillmentService, times(0)).getOrderById(anyLong());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerException() {
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(null, buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Error while processing fulfillmentResponseDto: {}",logsList.get(0).getMessage());
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerNullStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(null);
        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status not received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        verify(drcSalivaryOrderService, times(0)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    void testCreateFulfillmentOrderResponseListenerErrorStatus() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.ERROR);
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(buildOrderDetailsDTO());
        when(this.apiService.getUserDTO(anyLong())).thenReturn(getUserDto(vibrentID));

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("INFO", logsList.get(0).getLevel().toString());
        assertEquals("DRC Service: Relevant status received in fulfillmentResponseDto: {}",logsList.get(0).getMessage());
        verify(fulfillmentService, times(1)).getOrderById(anyLong());
        verify(drcSalivaryOrderService, times(1)).verifyAndSendFulfillmentOrderResponse(any(FulfillmentResponseDto.class), any(MessageHeaderDto.class), any(OrderDetailsDTO.class), any(ParticipantDto.class));
    }

    @Test
    @DisplayName("When no mailing address of user and fulfillment order response received " +
            "then log warn message")
    void testCreateFulfillmentOrderResponseListenerWhenNoMailingAddress() throws Exception {
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);
        when(fulfillmentService.getOrderById(anyLong())).thenReturn(buildOrderDetailsDTO());
        when(this.apiService.getUserDTO(anyLong())).thenReturn(null);

        Logger logger = (Logger) LoggerFactory.getLogger(FulfillmentResponseListener.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        fulfillmentResponseListener.listen(buildPayload(fulfillmentResponseDto), buildMessageHeaders());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(2).getLevel().toString());
        assertTrue(logsList.get(2).getMessage().contains("User does not have mailing address"));
    }

    private byte[] buildPayload(FulfillmentResponseDto fulfillmentResponseDto) throws JsonProcessingException {
        return JacksonUtil.getMapper().writeValueAsBytes(fulfillmentResponseDto);
    }

    private FulfillmentResponseDto createFulfillmentResponseDto() {
        FulfillmentResponseDto fulfillmentResponseDto = new FulfillmentResponseDto();

        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);
        fulfillmentResponseDto.setProgramID(programID);
        fulfillmentResponseDto.setVibrentID(vibrentID);
        OrderDto orderDto = new OrderDto();
        orderDto.setFulfillmentOrderID(100L);
        orderDto.setQuantity(1L);
        fulfillmentResponseDto.setOrder(orderDto);
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
        orderDetailsDTO.setParticipantId(22L);
        orderDetailsDTO.setSupplierOrderId("89234");
        return orderDetailsDTO;
    }

    private UserDTO getUserDto(Long id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setExternalId(EXTERNAL_ID);
        userDTO.setFirstName("SomeCatiName");
        userDTO.setMiddleInitial("L");
        userDTO.setLastName("CAtiLastName");
        userDTO.setDob(1323456879L);
        userDTO.setTestUser(false);
        userDTO.setEmail("abc@b.com");
        userDTO.setVerifiedPrimaryPhoneNumber("9874563210");

        var userAddressDTO = new UserAddressDTO();
        userAddressDTO.setType(AddressType.MAILING);
        userAddressDTO.setCity("city-1");
        userAddressDTO.setId(420L);
        List<UserAddressDTO> addressDTOList = List.of(userAddressDTO);
        userDTO.setUserAddresses(addressDTOList);
        return userDTO;
    }
}