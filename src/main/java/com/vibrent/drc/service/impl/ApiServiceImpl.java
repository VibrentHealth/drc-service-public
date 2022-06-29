package com.vibrent.drc.service.impl;

import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.acadia.web.rest.dto.form.ActiveFormVersionDTO;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;
import com.vibrent.acadia.web.rest.dto.form.FormVersionDTO;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.RestClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

import static com.vibrent.drc.constants.DrcConstant.*;

@Slf4j
@Service
public class ApiServiceImpl implements ApiService {

    private final String apiUrl;
    private final OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;
    private final RestClientUtil restClientUtil;


    public ApiServiceImpl(@Value("${vibrent.drc-service.acadiaApiUrl}") String apiUrl,
                          @Qualifier("keycloakDrcInternalCredentialsRestTemplate") OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate,
                          RestClientUtil restClientUtil) {
        this.apiUrl = apiUrl;
        this.keycloakDrcInternalCredentialsRestTemplate = keycloakDrcInternalCredentialsRestTemplate;
        this.restClientUtil = restClientUtil;

    }

    @Override
    @Cacheable(SALIVERY_BIOBANK_ADDRESS_CACHE)
    public String getBioBankAddress() {
        try {
            String url = apiUrl + BIOBANK_ADDRESS_DETAILS_API;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            return restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch biobank address details: " + e);
        }
    }

    @Override
    @Cacheable(SALIVERY_ORDER_DEVICE_CACHE)
    public String getDeviceDetails() {
        try {
            String url = apiUrl + SALIVARY_KIT_DETAILS_API;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            return restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch Salivary kit details: " + e);
        }
    }

    @Override
    public UserDTO getUserDTO(Long vibrentId) {
        if (vibrentId == null || vibrentId <= 0) {
            throw new BusinessProcessingException("Can't fetch User details for vibrentId: " + vibrentId);
        }
        try {
            String url = apiUrl + USER_DETAILS_API + "/" + vibrentId;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            String response = restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
            return JacksonUtil.getMapper().readValue(response, UserDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch User Details for vibrentId: " + vibrentId, e);
        }
    }

    @Override
    public List<FormEntryDTO> getUserFormEntryDTO(Long userId, String formName) {
        try {
            String url = apiUrl + GET_FORM_ENTRY_API;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            builder.queryParam("userId", userId);
            builder.queryParam("formName", formName);

            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            String response = restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
            return JacksonUtil.getMapper().readValue(response, JacksonUtil.getMapper().getTypeFactory().constructCollectionType(List.class, FormEntryDTO.class));
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch User FormEntryDTO for user: " + userId, e);
        }
    }

    @Override
    public FormVersionDTO getFormVersionById(Long id) {
        try {
            String url = apiUrl + GET_FORM_VERSION_API + id;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            String response = restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
            return JacksonUtil.getMapper().readValue(response, FormVersionDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch User FormVersion for id: " + id, e);
        }
    }

    @Override
    public ActiveFormVersionDTO getActiveFormVersionByFormId(Long formId) {
        try {
            String url = apiUrl + GET_ACTIVE_FORM_VERSION_API + formId;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            String response = restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
            return JacksonUtil.getMapper().readValue(response, ActiveFormVersionDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch active form version for formId: " + formId, e);
        }
    }

    @Override
    public UserSSNDTO getUserSsnByUserId(long userId) {
        try {
            String url = apiUrl + GET_SSN_INFO_API + userId;
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            OAuth2AccessToken accessToken = keycloakDrcInternalCredentialsRestTemplate.getAccessToken();
            String response = restClientUtil.getRequest(builder, restClientUtil.addAuthHeader(accessToken.getValue()));
            return JacksonUtil.getMapper().readValue(response, UserSSNDTO.class);
        } catch (Exception e) {
            throw new BusinessProcessingException("Failed to fetch SSN for user id: " + userId, e);
        }
    }

    @Override
    public FormEntryDTO getFormEntryDtoByFormName(@NotNull Long userId, @NotNull String formName) {
        List<FormEntryDTO> formEntryDTOList = getUserFormEntryDTO(userId, formName);
        Optional<FormEntryDTO> formEntryDTOOptional = formEntryDTOList.stream().filter(f -> (formName).equals(f.getFormName())).findFirst();
        return formEntryDTOOptional.orElse(null);
    }

}
