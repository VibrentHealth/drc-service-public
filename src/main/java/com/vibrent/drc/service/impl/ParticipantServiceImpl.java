package com.vibrent.drc.service.impl;

import com.vibrent.drc.cache.VibrentIdCacheManager;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.UserSearchParamRequestDTO;
import com.vibrent.drc.dto.UserSearchRequestDTO;
import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.HttpClientValidationException;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.RestClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static com.vibrent.drc.constants.DrcConstant.DEFAULT_PAGE;

@Slf4j
@Service
@CacheConfig(cacheNames = {DrcConstant.VIBRENTID_CACHE})
public class ParticipantServiceImpl implements ParticipantService {

    private final String apiUrl;
    private final OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;
    private final RestClientUtil restClientUtil;
    private final VibrentIdCacheManager vibrentIdCacheManager;


    public ParticipantServiceImpl(@Value("${vibrent.drc-service.acadiaApiUrl}") String apiUrl, OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate, RestClientUtil restClientUtil, VibrentIdCacheManager vibrentIdCacheManager) {
        this.apiUrl = apiUrl;
        this.keycloakDrcInternalCredentialsRestTemplate = keycloakDrcInternalCredentialsRestTemplate;
        this.restClientUtil = restClientUtil;
        this.vibrentIdCacheManager = vibrentIdCacheManager;
    }

    @Override
    public UserSearchResponseDTO getParticipantsByVibrentIds(List<String> vibrentIds) {
        try {
            String url = apiUrl + DrcConstant.USER_INFO_SEARCH_API;
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            UserSearchRequestDTO userSearchRequestDTO = getUserSearchRequestDTO(vibrentIds, UserInfoType.VIBRENT_ID, DEFAULT_PAGE, Integer.MAX_VALUE);
            HttpEntity<UserSearchRequestDTO> httpEntity = new HttpEntity<>(userSearchRequestDTO, restClientUtil.addAuthHeader(accessToken.getValue()));
            String responseBody = restClientUtil.postRequest(url, httpEntity);
            return JacksonUtil.getMapper().readValue(responseBody, UserSearchResponseDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("DRC Service: Failed to retrieve User Info for VibrentIds: " + vibrentIds, e);
        }
    }

    @Override
    public UserSearchResponseDTO getParticipantsByDrcIds(List<String> drcIds) {
        try {
            String url = apiUrl + DrcConstant.USER_INFO_SEARCH_API;
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            UserSearchRequestDTO userSearchRequestDTO = getUserSearchRequestDTO(drcIds, UserInfoType.EXTERNAL_ID, DEFAULT_PAGE, Integer.MAX_VALUE);
            HttpEntity<UserSearchRequestDTO> httpEntity = new HttpEntity<>(userSearchRequestDTO, restClientUtil.addAuthHeader(accessToken.getValue()));
            String responseBody = restClientUtil.postRequest(url, httpEntity);
            log.info("DRC-Service: Calling API to fetch Vibrent id");
            return JacksonUtil.getMapper().readValue(responseBody, UserSearchResponseDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("DRC Service: Failed to retrieve User Info for drcIds: " + drcIds, e);
        }
    }

    @Override
    @Cacheable(DrcConstant.VIBRENTID_CACHE)
    public Long getVibrentId(String externalId) {
        UserSearchResponseDTO userSearchResponseDTO = this.getParticipantsByDrcIds(Collections.singletonList(externalId));
        if(userSearchResponseDTO == null || CollectionUtils.isEmpty(userSearchResponseDTO.getResults())
            || CollectionUtils.isEmpty(userSearchResponseDTO.getResults().get(0))
            || StringUtils.isEmpty(userSearchResponseDTO.getResults().get(0).get(UserInfoType.VIBRENT_ID))) {
            log.warn("DRC Service: Couldn't fetch VibrentID from API for given externalID: {}", externalId);
            return null;
        }
        Map<UserInfoType, Object> userInfo = userSearchResponseDTO.getResults().get(0);
        return Long.valueOf(userInfo.get(UserInfoType.VIBRENT_ID).toString());
    }

    @Override
    public UserSearchResponseDTO getParticipants(List<String> vibrentIds, List<String> drcIds, Optional<String> startDate,
                                                 Optional<String> endDate, Optional<Integer> page, Optional<Integer> pageSize) {
        try {
            String url = apiUrl + DrcConstant.USER_INFO_SEARCH_API;
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            UserSearchRequestDTO userSearchRequestDTO = getUserSearchRequestDTO(vibrentIds, drcIds, startDate, endDate, page, pageSize);
            HttpEntity<UserSearchRequestDTO> httpEntity = new HttpEntity<>(userSearchRequestDTO, restClientUtil.addAuthHeader(accessToken.getValue()));
            String responseBody = restClientUtil.postRequest(url, httpEntity);
            return JacksonUtil.getMapper().readValue(responseBody, UserSearchResponseDTO.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new HttpClientValidationException(e.getResponseBodyAsString(), e.getStatusCode().value());
            } else {
                throw new BusinessProcessingException("DRC Service: Failed to retrieve User Info for VibrentIds " + vibrentIds +
                        ", drcIds: " + drcIds + ", startDate: " + startDate + ", endDate: " + endDate, e);
            }
        } catch (Exception e) {
            throw new BusinessProcessingException("DRC Service: Failed to retrieve User Info for VibrentIds " + vibrentIds +
                    ", drcIds: " + drcIds + ", startDate: " + startDate + ", endDate: " + endDate, e);
        }
    }

    @Override
    public void fetchAndCacheVibrentIds(Set<String> externalIds) {
        List<String> externalIdList = new ArrayList<>(externalIds);
        UserSearchResponseDTO participantsByDrcIds = getParticipantsByDrcIds(externalIdList);
        if (participantsByDrcIds == null || CollectionUtils.isEmpty(participantsByDrcIds.getResults())) {
            log.warn("DRC Service: Couldn't fetch VibrentID from API for given externalIDs");
            return;
        }
        List<Map<UserInfoType, Object>> responseList = participantsByDrcIds.getResults();

        for (String externalId : externalIdList) {
            Map<UserInfoType, Object> userInfoMap = getUserInfoMap(responseList, externalId);
            if (userInfoMap != null && userInfoMap.get(UserInfoType.VIBRENT_ID) != null) {
                vibrentIdCacheManager.addVibrentIdToCache(externalId, Long.valueOf(userInfoMap.get(UserInfoType.VIBRENT_ID).toString()));
            }
        }
    }

    /**
     * getUserSearchRequestDTO
     *
     * @param inputIds
     * @return
     */
    private static UserSearchRequestDTO getUserSearchRequestDTO(List<String> inputIds, UserInfoType userInfoType, int page, int pageSize) {
        UserSearchRequestDTO userSearchRequestDTO = new UserSearchRequestDTO();
        if(userInfoType == UserInfoType.VIBRENT_ID || userInfoType == UserInfoType.EXTERNAL_ID) {
            UserSearchParamRequestDTO userSearchParamRequestDTO = new UserSearchParamRequestDTO();
            userSearchParamRequestDTO.setInputType(userInfoType);

            Map<String, Object> filter = new HashMap<>();
            filter.put("ids", inputIds);
            userSearchParamRequestDTO.setInputParams(filter);
            userSearchRequestDTO.setSearchParams(Collections.singletonList(userSearchParamRequestDTO));
        }

        userSearchRequestDTO.setPage(page);
        userSearchRequestDTO.setPageSize(pageSize);

        userSearchRequestDTO.setRequiredTypes(new HashSet<>(Arrays.asList(UserInfoType.VIBRENT_ID, UserInfoType.EXTERNAL_ID, UserInfoType.TEST_PARTICIPANT)));
        return userSearchRequestDTO;
    }

    private static UserSearchRequestDTO getUserSearchRequestDTO(List<String> vibrentIds, List<String> drcIds, Optional<String> startDate,
                                                                Optional<String> endDate, Optional<Integer> page, Optional<Integer> pageSize) {
        UserSearchRequestDTO userSearchRequestDTO = new UserSearchRequestDTO();
        List<UserSearchParamRequestDTO> searchParams = new ArrayList<>();

        UserSearchParamRequestDTO vibrentIdParamRequestDTO = getIdsSearchParam(vibrentIds, UserInfoType.VIBRENT_ID);
        addSearchParamIfValid(searchParams, vibrentIdParamRequestDTO);

        UserSearchParamRequestDTO drcIdParamRequestDTO = getIdsSearchParam(drcIds, UserInfoType.EXTERNAL_ID);
        addSearchParamIfValid(searchParams, drcIdParamRequestDTO);

        UserSearchParamRequestDTO dateParamRequestDTO = getDateSearchParam(startDate, endDate);
        addSearchParamIfValid(searchParams, dateParamRequestDTO);

        addNotNullSearchParam(searchParams, UserInfoType.KEYCLOAK_ID, true);
        addNotNullSearchParam(searchParams, UserInfoType.ENTERPRISE_KEYCLOAK_ID, false);

        addIsActiveProgramSubscriptionParam(searchParams);

        //Set search params
        userSearchRequestDTO.setSearchParams(searchParams);

        page.ifPresent(userSearchRequestDTO::setPage);
        pageSize.ifPresent(userSearchRequestDTO::setPageSize);

        userSearchRequestDTO.setRequiredTypes(new HashSet<>(Arrays.asList(UserInfoType.VIBRENT_ID, UserInfoType.EXTERNAL_ID, UserInfoType.TEST_PARTICIPANT)));
        return userSearchRequestDTO;
    }

    private static UserSearchParamRequestDTO getIdsSearchParam(List<String> ids, UserInfoType userInfoType) {
        if(!CollectionUtils.isEmpty(ids)) {
            UserSearchParamRequestDTO userSearchParamRequestDTO = new UserSearchParamRequestDTO();
            userSearchParamRequestDTO.setInputType(userInfoType);
            Map<String, Object> filter = new HashMap<>();
            filter.put("ids", ids);
            userSearchParamRequestDTO.setInputParams(filter);
            return userSearchParamRequestDTO;
        }
        return null;
    }

    private static UserSearchParamRequestDTO getDateSearchParam(Optional<String> startDate, Optional<String> endDate) {
        String sDate = startDate.orElse(null);
        String eDate = endDate.orElse(null);
        if(!StringUtils.isEmpty(sDate) || !StringUtils.isEmpty(eDate)) {
            UserSearchParamRequestDTO userSearchParamRequestDTO = new UserSearchParamRequestDTO();
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("startDate", sDate);
            inputParams.put("endDate", eDate);
            userSearchParamRequestDTO.setInputParams(inputParams);
            userSearchParamRequestDTO.setInputType(UserInfoType.CREATED_DATE);
            return userSearchParamRequestDTO;
        }
        return null;
    }

    private static void addSearchParamIfValid(List<UserSearchParamRequestDTO> searchParams, UserSearchParamRequestDTO searchParamRequestDTO) {
        if (searchParamRequestDTO != null) {
            searchParams.add(searchParamRequestDTO);
        }
    }

    /**
     *
     * @param searchParams
     * @param userInfoType
     * @param paramValue
     */
    private static void addNotNullSearchParam(List<UserSearchParamRequestDTO> searchParams, UserInfoType userInfoType, boolean paramValue) {
        UserSearchParamRequestDTO userSearchParamRequestDTO = new UserSearchParamRequestDTO();
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("isNotNull", paramValue);

        userSearchParamRequestDTO.setInputType(userInfoType);
        userSearchParamRequestDTO.setInputParams(inputParams);
        searchParams.add(userSearchParamRequestDTO);
    }

    private static void addIsActiveProgramSubscriptionParam(List<UserSearchParamRequestDTO> searchParams) {
        UserSearchParamRequestDTO userSearchParamRequestDTO = new UserSearchParamRequestDTO();
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("isActive", true);

        userSearchParamRequestDTO.setInputType(UserInfoType.PROGRAM_SUBSCRIPTION);
        userSearchParamRequestDTO.setInputParams(inputParams);
        searchParams.add(userSearchParamRequestDTO);
    }

    /**
     * Get UserInfo map for the given external ID
     * @param responseList
     * @param externalId
     * @return
     */
    private static Map<UserInfoType, Object> getUserInfoMap(List<Map<UserInfoType, Object>> responseList, String externalId) {
        if(CollectionUtils.isEmpty(responseList) || StringUtils.isEmpty(externalId)) {
            return Collections.emptyMap();
        }

        for (Map<UserInfoType, Object> userInfoMap : responseList) {
            if (!CollectionUtils.isEmpty(userInfoMap) && userInfoMap.get(UserInfoType.EXTERNAL_ID) != null
                    && externalId.equals(userInfoMap.get(UserInfoType.EXTERNAL_ID).toString())) {
                return userInfoMap;
            }
        }
        return Collections.emptyMap();
    }
}
