package com.vibrent.drc.service.impl;

import com.vibrent.drc.cache.VibrentIdCacheManager;
import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.exception.HttpClientValidationException;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.RestClientUtil;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import lombok.SneakyThrows;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceImplTest {

    private ParticipantService participantService;

    private final String API_URL = "http://api:8080";

    @Mock
    private OAuth2RestTemplate keycloakApiClientCredentialsRestTemplate;

    @Mock
    private RestClientUtil restClientUtil;

    private DefaultOAuth2AccessToken oAuth2AccessToken;

    @Mock
    private VibrentIdCacheManager vibrentIdCacheManager;

    @Mock
    OAuth2ClientContext oAuth2ClientContext;

    @BeforeEach
    void setUp() {
        oAuth2AccessToken = new DefaultOAuth2AccessToken("access token");
        oAuth2AccessToken.setExpiration(Instant.now().plus(Duration.standardSeconds(300)).toDate());
        DRCRetryService retryService = new DRCRetryServiceImpl(getDrcConfigService());
        participantService = new ParticipantServiceImpl(API_URL, keycloakApiClientCredentialsRestTemplate, restClientUtil, vibrentIdCacheManager, retryService, "401,403,500,503");
    }


    @SneakyThrows
    @Test
    void getParticipantsByVibrentIds() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":\"v1\",\"EXTERNAL_ID\": \"p1\"}]}");

        UserSearchResponseDTO userSearchResponseDTO = participantService.getParticipantsByVibrentIds(Collections.singletonList("v1"));
        assertNotNull(userSearchResponseDTO);

        List<Map<UserInfoType, Object>> responseList = userSearchResponseDTO.getResults();
        assertNotNull(responseList);
        assertEquals(1, responseList.size());

        Map<UserInfoType, Object> userInfo = responseList.get(0);
        assertEquals(2, userInfo.size());
        assertEquals("v1", userInfo.get(UserInfoType.VIBRENT_ID));
        assertEquals("p1", userInfo.get(UserInfoType.EXTERNAL_ID));
    }

    @SneakyThrows
    @Test
    void getParticipantsByVibrentIdsWithException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new RuntimeException());

        Assert.assertThrows(
                RuntimeException.class,
                () -> participantService.getParticipantsByVibrentIds(Collections.singletonList("v1")));

    }

    @SneakyThrows
    @Test
    void getParticipantsByVibrentIdsWithExceptionForRetry() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {
        });

        Assert.assertThrows(
                RuntimeException.class,
                () -> participantService.getParticipantsByVibrentIds(Collections.singletonList("v1")));

    }

    @DisplayName("when API invoke to retrieve User Info And received null" +
            "Then Verify Exception thrown")
    @Test
    void getParticipants_throwsException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn(null);

        assertThrows(BusinessProcessingException.class, () -> participantService.getParticipants(Collections.singletonList("v1"), Collections.singletonList("d1"),
                java.util.Optional.of("23012021"), java.util.Optional.of("23012021"), java.util.Optional.of(1), java.util.Optional.of(2)));
    }

    @DisplayName("when API invoke to retrieve User Info And received Bad Request" +
            "Then Verify HTTP Client Validation Exception is thrown")
    @Test
    void getParticipants_throwsValidationException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request")).when(restClientUtil).postRequest(anyString(), any());

        assertThrows(HttpClientValidationException.class, () -> participantService.getParticipants(Collections.singletonList("v1"), Collections.singletonList("d1"),
                java.util.Optional.of("23012021"), java.util.Optional.of("23012021"), java.util.Optional.of(1), java.util.Optional.of(2)));
    }

    @DisplayName("when API invoke to retrieve User Info And received other client errors" +
            "Then Verify BusinessProcessingException is thrown")
    @Test
    void getParticipants_throwsBusinessProcessingException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        doThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Bad Request")).when(restClientUtil).postRequest(anyString(), any());

        assertThrows(BusinessProcessingException.class, () -> participantService.getParticipants(Collections.singletonList("v1"), Collections.singletonList("d1"),
                java.util.Optional.of("23012021"), java.util.Optional.of("23012021"), java.util.Optional.of(1), java.util.Optional.of(2)));
    }

    @DisplayName("when API invoke to retrieve User Info And received other client errors" +
            "Then Verify Retry execute")
    @Test
    void getParticipants_throwsHttpStatusCodeException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {
        });

        assertThrows(RuntimeException.class, () -> participantService.getParticipants(Collections.singletonList("v1"), Collections.singletonList("d1"),
                java.util.Optional.of("23012021"), java.util.Optional.of("23012021"), java.util.Optional.of(1), java.util.Optional.of(2)));
    }

    @Test
    void getParticipantsByDrcIds() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":\"v1\",\"EXTERNAL_ID\": \"p1\"}]}");

        UserSearchResponseDTO userSearchResponseDTO = participantService.getParticipantsByDrcIds(Collections.singletonList("p1"));
        assertNotNull(userSearchResponseDTO);

        List<Map<UserInfoType, Object>> responseList = userSearchResponseDTO.getResults();
        assertNotNull(responseList);
        assertEquals(1, responseList.size());

        Map<UserInfoType, Object> userInfo = responseList.get(0);
        assertEquals(2, userInfo.size());
        assertEquals("v1", userInfo.get(UserInfoType.VIBRENT_ID));
        assertEquals("p1", userInfo.get(UserInfoType.EXTERNAL_ID));
    }

    @Test
    void getParticipantsByDrcIdsWithException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new RuntimeException());

        Assert.assertThrows(
                RuntimeException.class,
                () -> participantService.getParticipantsByDrcIds(Collections.singletonList("p1")));
    }

    @Test
    void getParticipantsByDrcIdsWithExceptionRetry() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {
        });

        Assert.assertThrows(
                RuntimeException.class,
                    () -> participantService.getParticipantsByDrcIds(Collections.singletonList("p1")));
    }

    @Test
    void getParticipantsByDrcIdsWithAuthorizationException() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenThrow(new ResourceAccessException("401"));

        Assert.assertThrows(
                RuntimeException.class,
                () -> participantService.getParticipantsByDrcIds(Collections.singletonList("p1")));
    }


    @Test
    void testGetVibrentIdByDrcId() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":\"1\",\"EXTERNAL_ID\": \"p1\"}]}");

        Long vibrentId = participantService.getVibrentId("p1");
        assertNotNull(vibrentId);
        assertEquals(1L, vibrentId);
    }

    @Test
    void whenResponseListDoesNotContainDataThenVerifyVibrentId() {
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[]}");

        assertThrows(BusinessValidationException.class, ()->participantService.getVibrentId("p1"));
    }

    @DisplayName("When API invoke to retrieve User Info And received empty participant list from API" +
            "Then Verify warning is logged")
    @Test
    void TestFetchAndCacheVibrentIdsWhenEmptyResponseReceivedFromApi() {
        Set<String> externalId = Collections.singleton("p1");
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[]}");
        participantService.fetchAndCacheVibrentIds(externalId);
        verify(vibrentIdCacheManager, times(0)).addVibrentIdToCache(anyString(), anyLong());
    }

    @SneakyThrows
    @Test
    void whenAccessTokenExpiryTimeIsLessThan30SecThenVerifyLatestAccessTokenFetched() {
        DefaultOAuth2AccessToken tempOAuth2AccessToken = new DefaultOAuth2AccessToken("access token");
        tempOAuth2AccessToken.setExpiration(Instant.now().plus(Duration.standardSeconds(5)).toDate());
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(tempOAuth2AccessToken).thenReturn(oAuth2AccessToken);
        when(keycloakApiClientCredentialsRestTemplate.getOAuth2ClientContext()).thenReturn(oAuth2ClientContext);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[{\"VIBRENT_ID\":\"v1\",\"EXTERNAL_ID\": \"p1\"}]}");

        UserSearchResponseDTO userSearchResponseDTO = participantService.getParticipantsByVibrentIds(Collections.singletonList("v1"));
        assertNotNull(userSearchResponseDTO);
        verify(keycloakApiClientCredentialsRestTemplate, times(2)).getAccessToken();
    }



    private DRCConfigService  getDrcConfigService() {
        return new DRCConfigService() {
            @Override
            public boolean isRunPostProcessing() {
                return false;
            }

            @Override
            public String getDrcApiBaseUrl() {
                return null;
            }

            @Override
            public long getRetryNum() {
                return 3L;
            }

            @Override
            public long getRetryMaxInterval() {
                return 30L;
            }

            @Override
            public long getRetryStartingRetryDelayInterval() {
                return 1L;
            }

            @Override
            public int getRetryStartingOnWhichAttempt() {
                return 2;
            }

            @Override
            public boolean isDrcTestEnvironment() {
                return false;
            }
        };
    }
}