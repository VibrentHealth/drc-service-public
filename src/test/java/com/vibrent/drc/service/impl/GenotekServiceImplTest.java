package com.vibrent.drc.service.impl;

import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.GenotekService;
import com.vibrent.genotek.api.OrderInfoApi;
import com.vibrent.genotek.vo.OrderInfoDTO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenotekServiceImplTest {

    public static final String genotekUrl = "http://vxp-genotek:8080";

    @Mock
    private OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;
    @Mock
    private OAuth2AccessToken oAuth2AccessToken;
    @Mock
    BeanFactory beanFactory;
    @Mock
    OrderInfoApi orderInfoApi;
    @Mock
    Call<OrderInfoDTO> orderInfoDTOCall;

    private GenotekService genotekService;

    @BeforeEach
    void setUp() {
        genotekService = new GenotekServiceImpl(genotekUrl, beanFactory, keycloakDrcInternalCredentialsRestTemplate);
    }

    @Test
    @SneakyThrows
    void getSalivaryKitDetails() throws IOException {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(beanFactory.getBean(eq(OrderInfoApi.class), any(), anyString())).thenReturn(orderInfoApi);
        when(orderInfoApi.getOrderInformationByOrderId(anyLong())).thenReturn(orderInfoDTOCall);
        when(orderInfoDTOCall.execute()).thenReturn(buildOrderInfoDTO());

        OrderInfoDTO deviceDetails = genotekService.getDeviceDetails(100L);
        assertNotNull(deviceDetails);
        assertEquals("4081", deviceDetails.getItemCode());
        assertEquals("Salivary Order", deviceDetails.getOrderType());
    }

    @Test
    void getExceptionWhenOrderIdIsNull() {
        var exception = assertThrows(BusinessValidationException.class, () -> genotekService.getDeviceDetails(null));
        assertTrue(exception.getMessage().contains("Order ID is missing"));
    }

    private Response<OrderInfoDTO> buildOrderInfoDTO() {

        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        orderInfoDTO.setOrderType("Salivary Order");
        orderInfoDTO.setItemCode("4081");
        orderInfoDTO.setItemName("OGD-500.015");
        return Response.success(orderInfoDTO);
    }
}