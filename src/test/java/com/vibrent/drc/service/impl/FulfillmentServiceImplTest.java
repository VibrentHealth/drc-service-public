package com.vibrent.drc.service.impl;

import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.fulfillment.api.OrdersApi;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.fulfillment.dto.ProductDTO;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import retrofit2.Call;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceImplTest {

    public static final String fulfillmentUrl = "http://fulfillment:8080";

    @Mock
    OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;
    @Mock
    OAuth2AccessToken oAuth2AccessToken;
    @Mock
    BeanFactory beanFactory;
    @Mock
    OrdersApi ordersApi;
    @Mock
    Call<OrderDetailsDTO> orderDetailsDTOCall;
    @Mock
    DRCConfigService drcConfigService;
    DRCRetryService retryService;


    FulfillmentService fulfillmentService;
    final String retryForHttpStatusCodes = "504,403";

    @BeforeEach
    void setUp() {
        retryService = new DRCRetryServiceImpl(drcConfigService);
        fulfillmentService = new FulfillmentServiceImpl(fulfillmentUrl, beanFactory, retryService, retryForHttpStatusCodes, keycloakDrcInternalCredentialsRestTemplate);
    }

    @Test
    @SneakyThrows
    @DisplayName("check order details from fulfillment service based on order Id")
    void getOrderDetails() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(beanFactory.getBean(eq(OrdersApi.class), any(), anyString())).thenReturn(ordersApi);
        when(ordersApi.getOrderById(anyLong())).thenReturn(orderDetailsDTOCall);
        when(orderDetailsDTOCall.execute()).thenReturn(buildOrderDetailsDTO());
        when(drcConfigService.getRetryNum()).thenReturn(1L);

        OrderDetailsDTO orderDetails = fulfillmentService.getOrderById(100L);
        assertNotNull(orderDetails);
        assertEquals(106L, orderDetails.getProgramId());
        assertEquals("SHIPPED", orderDetails.getStatus());
    }

    @Test
    @DisplayName("Received exception when order Id is null")
    void getExceptionWhenOrderIdIsNull() {
        var exception = assertThrows(DrcException.class, () -> fulfillmentService.getOrderById(null));
        assertTrue(exception.getMessage().contains("Order ID is missing"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Received exception when Http status is not OKHttpStatus ")
    void getExceptionWhenOtherThanOkHttpStatus() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(beanFactory.getBean(eq(OrdersApi.class), any(), anyString())).thenReturn(ordersApi);
        when(ordersApi.getOrderById(anyLong())).thenReturn(orderDetailsDTOCall);
        when(drcConfigService.getRetryNum()).thenReturn(1L);
        when(orderDetailsDTOCall.execute()).thenReturn(Response.error(HttpStatus.BAD_REQUEST.value(), ResponseBody.create(MediaType.parse("error"), "BAD REQUEST")));

        var exception = assertThrows(DrcException.class, () -> fulfillmentService.getOrderById(100L));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Received retry exception when retry limit exceeded")
    void getExceptionWhenRetryApiCallFailed() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(beanFactory.getBean(eq(OrdersApi.class), any(), anyString())).thenReturn(ordersApi);
        when(ordersApi.getOrderById(anyLong())).thenReturn(orderDetailsDTOCall);
        when(orderDetailsDTOCall.execute()).thenReturn(Response.error(HttpStatus.GATEWAY_TIMEOUT.value(), ResponseBody.create(MediaType.parse("error"), "GATEWAY TIMEOUT")));
        when(drcConfigService.getRetryNum()).thenReturn(1L);

        var exception = assertThrows(BusinessProcessingException.class, () -> fulfillmentService.getOrderById(100L));
        assertTrue(exception.getMessage().contains("Retry api call limit has exceeded"));
    }

    private Response<OrderDetailsDTO> buildOrderDetailsDTO() {

        OrderDetailsDTO orderDetailsDTO = new OrderDetailsDTO();
        orderDetailsDTO.setId(100L);
        orderDetailsDTO.setProduct(new ProductDTO());
        orderDetailsDTO.setQuantity(1L);
        orderDetailsDTO.setStatus("SHIPPED");
        orderDetailsDTO.setProgramId(106L);
        return Response.success(orderDetailsDTO);
    }
}
