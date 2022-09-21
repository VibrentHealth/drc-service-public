package com.vibrent.drc.service.impl;

import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.GenotekService;
import com.vibrent.genotek.api.OrderInfoApi;
import com.vibrent.genotek.vo.OrderInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Service
public class GenotekServiceImpl implements GenotekService {

    private final Logger log = LoggerFactory.getLogger(GenotekServiceImpl.class);

    private String genotekUrl;
    private BeanFactory beanFactory;
    private final OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;

    public GenotekServiceImpl(@Value("${vibrent.drc-service.genotekUrl}") String genotekUrl, BeanFactory beanFactory,
                              @Qualifier("keycloakDrcInternalCredentialsRestTemplate") OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate) {
        this.genotekUrl = genotekUrl;
        this.beanFactory = beanFactory;
        this.keycloakDrcInternalCredentialsRestTemplate = keycloakDrcInternalCredentialsRestTemplate;
    }

    /**
     * This method fetch device details from Genotek service.
     *
     * @param orderId
     */
    @Override
    public OrderInfoDTO getDeviceDetails(Long orderId) {

        log.info("API-Genotek: Calling getDeviceDetails with orderId {}", orderId);
        OrderInfoDTO orderInfoDTO;
        try {
            if (orderId == null) {
                throw new BusinessValidationException("Order ID is missing");
            }
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            OrderInfoApi orderInfoApi = beanFactory.getBean(OrderInfoApi.class, accessToken.getValue(), genotekUrl);

            Call<OrderInfoDTO> orderResponseCall = orderInfoApi.getOrderInformationByOrderId(orderId);
            Response<OrderInfoDTO> response = orderResponseCall.execute();
            int code = response.code();
            if (code != HttpStatus.OK.value()) {
                throw new BusinessProcessingException(code +": "+ response.message());
            }
            orderInfoDTO = response.body();
        } catch (IOException e) {
            throw new BusinessProcessingException("Failed to fetch device details: " + e);
        }
        return orderInfoDTO;
    }

}
