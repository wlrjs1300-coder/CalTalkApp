package com.caltalk.backend;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

final class RenderEnvironmentNormalizer {

    private RenderEnvironmentNormalizer() {
    }

    static void normalize() {
        normalizeJdbcUrl("DB_URL");
        String customFrontendOrigin = asHttpsOrigin(System.getenv("CUSTOM_FRONTEND_ORIGIN"));
        if (customFrontendOrigin == null || customFrontendOrigin.isBlank()) {
            normalizeOrigin("FRONTEND_ORIGIN");
            normalizeOrigins("CORS_ALLOWED_ORIGINS");
            return;
        }
        System.setProperty("FRONTEND_ORIGIN", customFrontendOrigin);
        System.setProperty("CORS_ALLOWED_ORIGINS", mergeOrigins(
                asHttpsOrigins(System.getenv("CORS_ALLOWED_ORIGINS")),
                customFrontendOrigin));
    }

    static String asJdbcUrl(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            URI connectionUri = URI.create(trimmed);
            String host = connectionUri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("PostgreSQL connection URL must contain a host");
            }
            int port = connectionUri.getPort() == -1 ? 5432 : connectionUri.getPort();
            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(':')
                    .append(port)
                    .append(connectionUri.getRawPath());
            if (connectionUri.getRawQuery() != null) {
                jdbcUrl.append('?').append(connectionUri.getRawQuery());
            }
            return jdbcUrl.toString();
        }
        return trimmed;
    }

    static String asHttpsOrigin(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    static String asHttpsOrigins(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(RenderEnvironmentNormalizer::asHttpsOrigin)
                .collect(Collectors.joining(","));
    }

    static String mergeOrigins(String origins, String additionalOrigin) {
        return Arrays.stream(new String[] {origins, additionalOrigin})
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private static void normalizeJdbcUrl(String key) {
        overrideWhenChanged(key, asJdbcUrl(System.getenv(key)));
    }

    private static void normalizeOrigin(String key) {
        overrideWhenChanged(key, asHttpsOrigin(System.getenv(key)));
    }

    private static void normalizeOrigins(String key) {
        overrideWhenChanged(key, asHttpsOrigins(System.getenv(key)));
    }

    private static void overrideWhenChanged(String key, String normalizedValue) {
        String originalValue = System.getenv(key);
        if (normalizedValue != null && !normalizedValue.equals(originalValue)) {
            System.setProperty(key, normalizedValue);
        }
    }
}
