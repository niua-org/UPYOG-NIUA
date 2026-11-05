package org.egov.user.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
    @Qualifier("customAuthProvider")
    private AuthenticationProvider customAuthProvider;

    @Autowired
    @Qualifier("preAuthProvider")
    private AuthenticationProvider preAuthProvider;

    // REMOVED: Duplicate bean - CustomAuthenticationManager class already provides this bean
    // @Bean("customAuthenticationManager")
    // public AuthenticationManager customAuthenticationManager() {
    //     return new ProviderManager(Arrays.asList(customAuthProvider, preAuthProvider));
    // }

    @Bean
    @Order(1)
    public SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/oauth/**")  // OAuth endpoints
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // 1. Infrastructure, Health & API Documentation
                .requestMatchers("/actuator/**", "/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 2. OAuth2 & Authentication Endpoints (Unauthenticated / Public)
                .requestMatchers("/oauth2/**", "/login", "/user/oauth/**", "/_oauth2/**", "/digilocker/oauth/token").permitAll()
                .requestMatchers("/users/v2/_login").permitAll()
                .requestMatchers("/password/nologin/_update").permitAll()

                // 3. Previous / Existing Endpoints (Preserved from original baseline)
                .requestMatchers("/_details", "/_search", "/v1/_search",
                               "/citizen/_create", "/users/_createnovalidate",
                               "/users/_updatenovalidate",
                               "/users/v2/_create", "/users/v2/_search", "/users/v2/_update").permitAll()
                .requestMatchers("/password/_update", "/_logout").permitAll()

                // 4. Newly Added Endpoints: ONLY those publicly accessible without any role (Registration / Pre-Auth)
                .requestMatchers("/profile/_create", "/profile/_init",
                               "/profile/aadhaar/_verify", "/profile/aadhaar/_verifyProfile").permitAll()

                .anyRequest().authenticated()
            )
            .build();
    }

    // API endpoints that require authentication
    @Bean
    @Order(5)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(request ->
                // Only match endpoints that explicitly need authentication
                request.getRequestURI().startsWith("/api/") ||
                request.getRequestURI().startsWith("/secure/")
            )
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaque -> {
                // Configure opaque token introspection if needed
                opaque.introspectionUri("http://localhost:8080/user/oauth/introspect");
                opaque.introspectionClientCredentials("egov-user-client", "egov-user-secret");
            }))
            .build();
    }
}
