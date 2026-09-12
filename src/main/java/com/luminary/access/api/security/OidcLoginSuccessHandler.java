package com.luminary.access.api.security;

import com.luminary.access.application.LoginService;
import com.luminary.access.infrastructure.identity.OidcClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

import jakarta.servlet.ServletException;

/**
 * After the provider exchange, maps the verified identity into the local User
 * and opens the BFF server session (successful login => redirect to the
 * application).
 */
public class OidcLoginSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginService loginService;

    public OidcLoginSuccessHandler(LoginService loginService,
                                   String postLoginRedirect) {
        this.loginService = loginService;
        setDefaultTargetUrl(postLoginRedirect);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            loginService.login(OidcClaims.from(oidcUser));
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}