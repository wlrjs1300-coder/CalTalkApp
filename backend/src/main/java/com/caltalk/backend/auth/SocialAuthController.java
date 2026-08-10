package com.caltalk.backend.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/social")
public class SocialAuthController {

    private final SocialOAuthProperties properties;

    public SocialAuthController(SocialOAuthProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/providers")
    public List<SocialProviderResponse> providers() {
        List<SocialProviderResponse> providers = new ArrayList<>();
        if (!properties.isEnabled()) return providers;
        addIfConfigured(providers, "kakao", "카카오", properties.getKakao());
        addIfConfigured(providers, "naver", "네이버", properties.getNaver());
        addIfConfigured(providers, "google", "Google", properties.getGoogle());
        return providers;
    }

    private void addIfConfigured(
            List<SocialProviderResponse> providers,
            String id,
            String name,
            SocialOAuthProperties.Provider provider
    ) {
        if (provider.isConfigured()) providers.add(new SocialProviderResponse(id, name));
    }
}
