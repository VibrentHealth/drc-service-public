package com.vibrent.drc.service.impl;

import com.vibrent.drc.cache.VibrentIdCacheManager;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.RestClientUtil;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class ParticipantServiceImplIT {

    private static final String CACHE_SPECIFICATION = "maximumSize=500";
    private static final String KEY1 = "P324234";

    @SpyBean
    private RestClientUtil restClientUtil;


    @Autowired
    private ParticipantService participantService;

    @Configuration
    @EnableCaching
    static class Config {

        @MockBean
        RestClientUtil restClientUtil;

        @MockBean
        private OAuth2RestTemplate keycloakApiClientCredentialsRestTemplate;

        @MockBean
        private OAuth2AccessToken oAuth2AccessToken;

        @Mock
        private VibrentIdCacheManager vibrentIdCacheManager;

        private String apiUrl = "http://localhost:8080";

        @Autowired
        DRCRetryService retryService;

        @Bean
        ParticipantService participantService() {
            when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
            when(oAuth2AccessToken.getExpiresIn()).thenReturn(300);
            when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"requestedIdType\":\"VIBRENT_ID\",\"responseList\":{\"P324234\":{\"VIBRENT_ID\":\"1\",\"EXTERNAL_ID\": \"P324234\"}}}");

            return new ParticipantServiceImpl(apiUrl, keycloakApiClientCredentialsRestTemplate, restClientUtil, vibrentIdCacheManager, retryService, null);
        }
    }

    @Test
    public void when_callToGetVibrentIdInvoked_then_valuesAreStoredInCache() {
        participantService.getVibrentId(KEY1);
        participantService.getVibrentId(KEY1);
        verify(restClientUtil, times(1)).postRequest(any(), any());
    }
}