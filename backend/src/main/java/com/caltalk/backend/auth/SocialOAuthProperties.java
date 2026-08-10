package com.caltalk.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "caltalk.oauth")
public class SocialOAuthProperties {

    private boolean enabled;
    private final Provider google = new Provider();
    private final Provider kakao = new Provider();
    private final Provider naver = new Provider();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Provider getGoogle() {
        return google;
    }

    public Provider getKakao() {
        return kakao;
    }

    public Provider getNaver() {
        return naver;
    }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
