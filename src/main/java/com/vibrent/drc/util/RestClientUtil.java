package com.vibrent.drc.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@AllArgsConstructor
@Component
@Slf4j
public class RestClientUtil {

    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_VALUE_APPLICATION_JSON = "application/json";

    private RestTemplate restTemplate;

    /**
     * This method makes a Get call to the given url
     *
     * @param builder
     * @param httpHeaders
     * @return
     */
    public String getRequest(UriComponentsBuilder builder, HttpHeaders httpHeaders) {
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class).getBody();
    }

    /**
     * This method makes a Get call to the given url
     *
     * @param builder
     * @param httpEntity
     * @return
     */
    public String getRequest(UriComponentsBuilder builder, HttpEntity<?> httpEntity) {
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, String.class).getBody();
    }

    /**
     * @param url
     * @param entity
     * @return
     */
    public String postRequest(String url, HttpEntity<?> entity) {
        return restTemplate.postForObject(url, entity, String.class);
    }

    /**
     * creates the authorization headers
     *
     * @param authenticationToken
     * @return
     */
    public HttpHeaders addAuthHeader(String authenticationToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER_KEY, BEARER + " " + authenticationToken);
        headers.add(HEADER_KEY_CONTENT_TYPE, HEADER_VALUE_APPLICATION_JSON);
        return headers;
    }
}