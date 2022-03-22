package com.vibrent.drc.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.drc.cache.VibrentIdCacheManager;
import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.HttpClientValidationException;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.RestClientUtil;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceImplTest {

    private ParticipantService participantService;

    private final String API_URL = "http://api:8080";

    @Mock
    private OAuth2RestTemplate keycloakApiClientCredentialsRestTemplate;

    @Mock
    private RestClientUtil restClientUtil;

    @Mock
    private OAuth2AccessToken oAuth2AccessToken;

    @Mock
    private VibrentIdCacheManager vibrentIdCacheManager;

    @BeforeEach
    void setUp() {
        participantService = new ParticipantServiceImpl(API_URL, keycloakApiClientCredentialsRestTemplate, restClientUtil, vibrentIdCacheManager);
    }

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

        assertNull(participantService.getVibrentId("p1"));
    }

    @DisplayName("When API invoke to retrieve User Info And received empty participant list from API" +
            "Then Verify warning is logged")
    @Test
    void TestFetchAndCacheVibrentIdsWhenEmptyResponseReceivedFromApi() {
        Set<String> externalId = Collections.singleton("p1");
        when(keycloakApiClientCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.postRequest(anyString(), any())).thenReturn("{\"results\":[]}");
        Logger logger = (Logger) LoggerFactory.getLogger(ParticipantServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        participantService.fetchAndCacheVibrentIds(externalId);
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("WARN", logsList.get(1).getLevel().toString());
        assertEquals("DRC Service: Couldn't fetch VibrentID from API for given externalIDs", logsList.get(1).getMessage());
    }

}