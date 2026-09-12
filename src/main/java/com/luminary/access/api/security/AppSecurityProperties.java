package com.luminary.access.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * BFF security surface: CORS origins trusted by the SPA and the post-login
 * redirect for the browser.
 */
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private List<String> allowedOrigins = List.of("http://localhost:4200");

    private String postLoginRedirect = "http://localhost:4200/";

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getPostLoginRedirect() {
        return postLoginRedirect;
    }

    public void setPostLoginRedirect(String postLoginRedirect) {
        this.postLoginRedirect = postLoginRedirect;
    }
}