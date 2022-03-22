package com.vibrent.drc.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

import java.text.MessageFormat;

@Configuration
@EnableOAuth2Client
public class OAuth2Configuration {

    @Value("${vibrent.oidc-auth.keycloak.participantRealm}")
    private String keycloakParticipantRealm;

    @Value("${vibrent.oidc-auth.keycloak.baseAuthUrl}")
    private String keycloakBaseUrl;

    @Value("${vibrent.oidc-auth.keycloak.drcInternalClientId}")
    private String drcInternalClientId;

    @Value("${vibrent.oidc-auth.keycloak.drcInternalClientSecret}")
    private String drcInternalClientSecret;


    private static final String KEYCLOAK_TOKEN_ENDPOINT = "{0}/realms/{1}/protocol/openid-connect/token";


    @Bean
    public OAuth2ProtectedResourceDetails keycloakDrcInternalCredentials() {
        ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setId(keycloakParticipantRealm);
        details.setClientId(drcInternalClientId);
        details.setClientSecret(drcInternalClientSecret);
        String keycloakTokenEndpoint = MessageFormat.format(KEYCLOAK_TOKEN_ENDPOINT, keycloakBaseUrl, keycloakParticipantRealm);
        details.setAccessTokenUri(keycloakTokenEndpoint);
        details.setTokenName("keycloak_oauth_token");
        details.setGrantType("client_credentials");
        return details;
    }

    @Bean(name = "keycloakDrcInternalCredentialsRestTemplate")
    public OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate() {
        return new OAuth2RestTemplate(keycloakDrcInternalCredentials());
    }
}
