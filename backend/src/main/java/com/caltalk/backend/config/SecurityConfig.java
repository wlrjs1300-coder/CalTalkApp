package com.caltalk.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

import com.caltalk.backend.common.error.SecurityErrorHandler;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorHandler securityErrorHandler
    ) throws Exception {
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/api/v1/auth/signup",
                        "/api/v1/auth/login"
                )
        ).authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/signup",
                        "/api/v1/auth/login",
                        "/api/v1/auth/logout"
                ).permitAll()
                .requestMatchers(
                        "/api/v1/health",
                        "/actuator/health",
                        "/actuator/health/**"
                ).permitAll()
                .anyRequest().authenticated()
        ).exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityErrorHandler securityErrorHandler(ObjectMapper objectMapper) {
        return new SecurityErrorHandler(objectMapper);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }
}
