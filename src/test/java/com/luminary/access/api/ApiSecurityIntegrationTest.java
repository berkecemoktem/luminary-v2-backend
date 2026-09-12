package com.luminary.access.api;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.AuthIdentity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.TenantRepository;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BFF security surface tests over the full application context backed by an
 * embedded H2 (test profile: {@code src/test/resources/application.yml}).
 * The BFF session marker is stubbed; its real (Spring Session) wiring is
 * verified by unit tests and by the runtime browser-style verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    private static final String ISSUER = "https://issuer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @MockBean
    private CurrentSession currentSession;

    private UserEntity user;
    private String tenantId;

    @BeforeEach
    void seedAndStubSession() {
        user = userRepository.save(UserEntity.create(
                new AuthIdentity("https://issuer", "sub-abc"),
                "api@luminary.dev", "Api"));
        TenantEntity personal = tenantRepository.save(
                TenantEntity.personal("Api"));
        tenantId = personal.getId().toString();
        membershipRepository.save(MembershipEntity.create(personal, user,
                MembershipRole.STUDENT));

        when(currentSession.userId())
                .thenReturn(Optional.of(new UserId(user.getId())));
        when(currentSession.activeTenant())
                .thenReturn(Optional.of(new TenantId(personal.getId())));
    }

    @Test
    void unauthenticatedApiRequest_returnsProblemJson401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("authentication-required"));
    }

    @Test
    void unauthenticatedBrowserRequest_redirectsToOidc() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/luminary"));
    }

    @Test
    void authenticatedSession_readsProfileAndWorkspaces() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .with(oidcLogin().idToken(token ->
                                token.subject("sub-abc").issuer(ISSUER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("api@luminary.dev"))
                .andExpect(jsonPath("$.activeWorkspace").value(tenantId))
                .andExpect(jsonPath("$.workspaces[0].role")
                        .value("STUDENT"));
    }

    @Test
    void switchWorkspace_requiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/me/active-workspace")
                        .with(oidcLogin().idToken(token ->
                                token.subject("sub-abc").issuer(ISSUER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/me/active-workspace")
                        .with(oidcLogin().idToken(token ->
                                token.subject("sub-abc").issuer(ISSUER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"" + tenantId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantId));
    }

    @Test
    void logout_withCsrf_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("SESSION")));
    }
}