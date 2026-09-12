package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.AccountStatus;
import com.luminary.access.domain.AuthIdentity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.TenantType;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.TenantRepository;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.access.infrastructure.identity.OidcClaims;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private UserRepository userRepository;
    private TenantRepository tenantRepository;
    private MembershipRepository membershipRepository;
    private CurrentSession currentSession;
    private AccessAppProperties properties;
    private LoginService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tenantRepository = mock(TenantRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        currentSession = mock(CurrentSession.class);
        properties = new AccessAppProperties();
        service = new LoginService(userRepository, tenantRepository,
                membershipRepository, currentSession, properties);
    }

    private static AuthIdentity identity() {
        return new AuthIdentity("https://idp.test/luminary", "sub-1");
    }

    private static OidcClaims claims() {
        return new OidcClaims(identity(), "Student@Luminary.Dev",
                "Student", true);
    }

    @Test
    void firstLogin_createsTenantlessUser() {
        when(userRepository.findByAuthIssuerAndAuthSubject(
                identity().issuer(), identity().subject()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserId(any(UUID.class)))
                .thenReturn(List.of());

        UserId userId = service.login(claims());

        assertThat(userId.value()).isNotNull();
        verify(userRepository).save(any(UserEntity.class));
        verify(tenantRepository, never()).save(any(TenantEntity.class));
        verify(membershipRepository, never()).save(
                any(MembershipEntity.class));
        verify(currentSession).authenticate(any(UserId.class), isNull());
    }

    @Test
    void allowlistedAdmin_getsInstitutionWorkspaceProvisioned() {
        properties.getDevFirstAdmin().setEnabled(true);
        properties.getDevFirstAdmin().setEmailAllowlist(
                Set.of("admin@luminary.dev"));

        OidcClaims adminClaims = new OidcClaims(
                new AuthIdentity("https://idp.test/luminary", "sub-admin"),
                "admin@luminary.dev", "Admin", true);
        when(userRepository.findByAuthIssuerAndAuthSubject(
                adminClaims.identity().issuer(),
                adminClaims.identity().subject()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.save(any(TenantEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.save(any(MembershipEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserId(any(UUID.class)))
                .thenReturn(List.of());

        service.login(adminClaims);

        verify(userRepository).save(any(UserEntity.class));
        verify(tenantRepository).save(argThat(
                t -> t.getType() == TenantType.INSTITUTION));
        verify(membershipRepository).save(argThat(
                m -> m.getRole() == MembershipRole.INSTITUTION_ADMIN));
    }

    @Test
    void allowlistedAdminWithoutDevAdminEnabled_getsInstitutionWorkspaceOnly() {
        OidcClaims adminClaims = new OidcClaims(
                new AuthIdentity("https://idp.test/luminary", "sub-admin"),
                "admin@luminary.dev", "Admin", true);
        when(userRepository.findByAuthIssuerAndAuthSubject(
                adminClaims.identity().issuer(),
                adminClaims.identity().subject()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserId(any(UUID.class)))
                .thenReturn(List.of());

        service.login(adminClaims);

        verify(tenantRepository, never()).save(any(TenantEntity.class));
        verify(membershipRepository, never()).save(
                any(MembershipEntity.class));
    }

    @Test
    void notAllowlistedAdmin_isProvisionedNothingEvenWhenEnabled() {
        properties.getDevFirstAdmin().setEnabled(true);
        properties.getDevFirstAdmin().setEmailAllowlist(
                Set.of("someone.else@luminary.dev"));

        OidcClaims claims = claims();
        when(userRepository.findByAuthIssuerAndAuthSubject(
                claims.identity().issuer(), claims.identity().subject()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findByUserId(any(UUID.class)))
                .thenReturn(List.of());

        service.login(claims);

        verify(tenantRepository, never()).save(any(TenantEntity.class));
        verify(membershipRepository, never()).save(
                any(MembershipEntity.class));
    }

    @Test
    void disabledUser_isRejected() {
        UserEntity disabled = mock(UserEntity.class);
        when(disabled.getStatus()).thenReturn(AccountStatus.DISABLED);
        when(disabled.getEmail()).thenReturn("s@luminary.dev");
        when(disabled.getDisplayName()).thenReturn("Student");
        when(userRepository.findByAuthIssuerAndAuthSubject(
                identity().issuer(), identity().subject()))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.login(claims()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getStatus().value()).isEqualTo(403));
    }

    @Test
    void existingAdminMember_doesNotProvisionAnything() {
        AuthIdentity id = identity();
        UserEntity existing = UserEntity.create(id, "admin@luminary.dev",
                "Admin");
        TenantEntity institution = TenantEntity.institution(
                "Sınav Okulları Ankara", "Europe/Istanbul");
        MembershipEntity membership =
                MembershipEntity.create(institution, existing,
                        MembershipRole.INSTITUTION_ADMIN);
        when(userRepository.findByAuthIssuerAndAuthSubject(
                id.issuer(), id.subject()))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.findByUserId(existing.getId()))
                .thenReturn(List.of(membership));

        service.login(claims());

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(tenantRepository, never()).save(any(TenantEntity.class));
        verify(membershipRepository, never()).save(
                any(MembershipEntity.class));
    }
}