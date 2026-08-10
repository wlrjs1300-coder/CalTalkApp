package com.caltalk.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RenderEnvironmentNormalizerTests {

    @Test
    void convertsRenderPostgresUrlToJdbcUrl() {
        assertThat(RenderEnvironmentNormalizer.asJdbcUrl("postgresql://user:pass@db:5432/caltalk"))
                .isEqualTo("jdbc:postgresql://user:pass@db:5432/caltalk");
    }

    @Test
    void preservesExistingJdbcUrl() {
        assertThat(RenderEnvironmentNormalizer.asJdbcUrl("jdbc:postgresql://localhost:5432/caltalk"))
                .isEqualTo("jdbc:postgresql://localhost:5432/caltalk");
    }

    @Test
    void convertsRenderHostnameToHttpsOrigin() {
        assertThat(RenderEnvironmentNormalizer.asHttpsOrigin("caltalk-frontend.onrender.com"))
                .isEqualTo("https://caltalk-frontend.onrender.com");
    }

    @Test
    void normalizesCommaSeparatedOrigins() {
        assertThat(RenderEnvironmentNormalizer.asHttpsOrigins(
                "caltalk-frontend.onrender.com, https://caltalk.example.com"))
                .isEqualTo("https://caltalk-frontend.onrender.com,https://caltalk.example.com");
    }

    @Test
    void mergesRenderAndCustomOriginsWithoutDuplicates() {
        assertThat(RenderEnvironmentNormalizer.mergeOrigins(
                "https://caltalk-frontend.onrender.com,https://caltalk.example.com",
                "https://caltalk.example.com"))
                .isEqualTo("https://caltalk-frontend.onrender.com,https://caltalk.example.com");
    }
}
