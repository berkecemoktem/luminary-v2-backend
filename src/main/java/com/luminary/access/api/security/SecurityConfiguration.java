package com.luminary.access.api.security;

import com.luminary.access.application.LoginService;
import com.luminary.shared.error.ApiProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.function.Supplier;

/**
 * BFF security: OIDC login, session cookie, CSRF via a deferred cookie token,
 * CORS for the SPA, and an API-aware 401/403 error surface.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfiguration {

    private static final String[] PUBLIC = {
            "/actuator/health", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/webjars/**", "/error",
            "/login/**", "/oauth2/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginService loginService,
            AppSecurityProperties props,
            ApiProblemWriter problemWriter) throws Exception {

        OidcLoginSuccessHandler successHandler =
                new OidcLoginSuccessHandler(loginService,
                        props.getPostLoginRedirect());

        http
                .cors(Customizer.withDefaults())
                .requestCache(cache -> cache.requestCache(apiAwareRequestCache()))
                .csrf(csrf -> csrf
                        // Cookie-based CSRF for the SPA: the raw XSRF-TOKEN
                        // cookie is echoed back in the X-XSRF-TOKEN header so
                        // no XOR encoding is applied.
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler()))
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new ApiAwareAuthenticationEntryPoint(
                                        problemWriter))
                        .accessDeniedHandler(
                                apiAccessDeniedHandler(problemWriter)))
                .oauth2Login(login -> login.successHandler(successHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(
                                        HttpStatus.NO_CONTENT))
                        .deleteCookies("SESSION"));

        return http.build();
    }

    private static CsrfTokenRequestAttributeHandler csrfRequestHandler() {
        return new EagerCsrfTokenRequestHandler();
    }

    /**
     * Never remember API requests as the post-login target: the SPA probes
     * {@code /api/v1/me} on every page load while unauthenticated, and being
     * redirected back to a JSON endpoint after login is useless.
     */
    private static HttpSessionRequestCache apiAwareRequestCache() {
        return new HttpSessionRequestCache() {
            @Override
            public void saveRequest(HttpServletRequest request,
                                    HttpServletResponse response) {
                if (!request.getRequestURI().startsWith("/api/")) {
                    super.saveRequest(request, response);
                }
            }
        };
    }

    /**
     * Generates and persists the XSRF-TOKEN cookie on the first request
     * (typically an authenticated GET) so the SPA can echo it back in the
     * X-XSRF-TOKEN header without a rejected first POST.
     */
    private static final class EagerCsrfTokenRequestHandler
            extends CsrfTokenRequestAttributeHandler {
        @Override
        public void handle(HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            csrfToken.get();
            super.handle(request, response, csrfToken);
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            AppSecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.getAllowedOrigins());
        cfg.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    AccessDeniedHandler apiAccessDeniedHandler(
            ApiProblemWriter problemWriter) {
        return (request, response, accessDeniedException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                problemWriter.write(request, response, HttpStatus.FORBIDDEN,
                        "access-denied",
                        "Not authorized for this operation.");
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }
}
