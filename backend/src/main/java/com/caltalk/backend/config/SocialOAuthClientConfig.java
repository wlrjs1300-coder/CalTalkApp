package com.caltalk.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import com.caltalk.backend.auth.SocialOAuthProperties;

@Configuration
@ConditionalOnProperty(name = "caltalk.oauth.enabled", havingValue = "true")
public class SocialOAuthClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            SocialOAuthProperties properties
    ) {
        List<ClientRegistration> registrations = new ArrayList<>();
        addIfConfigured(registrations, google(properties.getGoogle()));
        addIfConfigured(registrations, kakao(properties.getKakao()));
        addIfConfigured(registrations, naver(properties.getNaver()));
        if (registrations.isEmpty()) {
            throw new IllegalStateException(
                    "SOCIAL_LOGIN_ENABLED=true requires at least one complete provider credential pair"
            );
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private void addIfConfigured(List<ClientRegistration> registrations, ClientRegistration value) {
        if (value != null) registrations.add(value);
    }

    private ClientRegistration google(SocialOAuthProperties.Provider provider) {
        if (!provider.isConfigured()) return null;
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .scope("openid", "profile", "email")
                .clientName("Google")
                .build();
    }

    private ClientRegistration kakao(SocialOAuthProperties.Provider provider) {
        if (!provider.isConfigured()) return null;
        return ClientRegistration.withRegistrationId("kakao")
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }

    private ClientRegistration naver(SocialOAuthProperties.Provider provider) {
        if (!provider.isConfigured()) return null;
        return ClientRegistration.withRegistrationId("naver")
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("response")
                .clientName("Naver")
                .build();
    }
}
