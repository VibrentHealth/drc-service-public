package com.vibrent.drc.service.impl;

import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.exception.DrcException;
import com.vibrent.drc.service.FulfillmentService;
import com.vibrent.fulfillment.api.OrdersApi;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrenthealth.drcutils.exception.RecoverableNetworkException;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Service
@Slf4j
public class FulfillmentServiceImpl implements FulfillmentService {

    private final String fulfillmentUrl;
    private final BeanFactory beanFactory;
    private final DRCRetryService retryService;
    private final List<Class<? extends Exception>> retryExceptionsList = List.of(RecoverableNetworkException.class);

    private final String retryForHttpStatusCodes;
    private final OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;

    public FulfillmentServiceImpl(@Value("${vibrent.drc-service.fulfillmentUrl}") String fulfillmentUrl, BeanFactory beanFactory,
                                  DRCRetryService retryService, @Value("${vibrent.drc-service.retryApiCall.retryForHttpStatusCode}") String retryForHttpStatusCodes, @Qualifier("keycloakDrcInternalCredentialsRestTemplate") OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate) {
        this.fulfillmentUrl = fulfillmentUrl;
        this.beanFactory = beanFactory;
        this.retryService = retryService;
        this.retryForHttpStatusCodes = retryForHttpStatusCodes;
        this.keycloakDrcInternalCredentialsRestTemplate = keycloakDrcInternalCredentialsRestTemplate;
    }

    /**
     * This method fetch order details from Fulfillment service.
     *
     * @param orderId
     */
    @Override
    public OrderDetailsDTO getOrderById(Long orderId) throws DrcException {

        log.info("API-Fulfillment: Calling getOrderById with orderId {}", orderId);

        OrderDetailsDTO orderDetailsDTO = null;
        try {
            if (orderId == null) {
                throw new BusinessValidationException("Order ID is missing");
            }

            orderDetailsDTO = retryService.executeWithRetryForExceptions(() -> callGetOrderById(orderId), retryExceptionsList);
        } catch (IOException e) {
            throw new BusinessProcessingException("Failed to fetch order details: " + e);
        } catch (RecoverableNetworkException e) {
            throw new BusinessProcessingException("Retry api call limit has exceeded and failed to fetch order details: " + e);
        } catch (Exception e) {
            throw new DrcException(e.getMessage(), e);
        }
        return orderDetailsDTO;
    }

    private OrderDetailsDTO callGetOrderById(Long orderId) throws IOException, RecoverableNetworkException {

        OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
        OrdersApi ordersApi = beanFactory.getBean(OrdersApi.class, accessToken.getValue(), fulfillmentUrl);

        Call<OrderDetailsDTO> orderResponseCall = ordersApi.getOrderById(orderId);
        Response<OrderDetailsDTO> response = orderResponseCall.execute();
        int code = response.code();

        var retryForHttpStatusCodeList = List.of(retryForHttpStatusCodes.split(","));
        if (retryForHttpStatusCodeList.contains(String.valueOf(code))) {
            log.debug("Retry for Http Status: " + code);
            throw new RecoverableNetworkException("Retry for Http Status " + code + ": " + response.message());
        } else if (code != HttpStatus.OK.value()) {
            throw new BusinessProcessingException(code + ": " + response.message());
        }

        return response.body();
    }

}
