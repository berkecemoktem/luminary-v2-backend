package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.AccountStatus;
import com.luminary.access.domain.AuthIdentity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.TenantStatus;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.TenantRepository;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.access.infrastructure.identity.OidcClaims;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Maps an OIDC {@code issuer + subject} to the local User (E03-03) and opens
 * the BFF server session. Tenants are institutions only: a new user is created
 * without any workspace and stays tenantless until a school invitation is
 * accepted or (dev only) a first admin is provisioned.
 */
@Service
public class LoginService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MembershipRepository membershipRepository;
    private final CurrentSession currentSession;
    private final AccessAppProperties properties;

    public LoginService(UserRepository userRepository,
                        TenantRepository tenantRepository,
                        MembershipRepository membershipRepository,
                        CurrentSession currentSession,
                        AccessAppProperties properties) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.currentSession = currentSession;
        this.properties = properties;
    }

    @Transactional
    public UserId login(OidcClaims claims) {
        AuthIdentity identity = claims.identity();
        UserEntity user = userRepository
                .findByAuthIssuerAndAuthSubject(identity.issuer(),
                        identity.subject())
                .orElseGet(() -> userRepository.save(
                        UserEntity.create(identity,
                                normalizeEmail(claims.email()),
                                claims.displayName())));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("user-disabled",
                    "This account is disabled. Contact your administrator.");
        }

        if (membershipRepository.findByUserId(user.getId()).isEmpty()) {
            provisionFirstAdmin(user);
        }

        currentSession.authenticate(new UserId(user.getId()),
                firstActiveInstitution(user.getId()));
        return new UserId(user.getId());
    }

    private void provisionFirstAdmin(UserEntity user) {
        AccessAppProperties.DevFirstAdmin admin =
                properties.getDevFirstAdmin();
        if (!admin.isEnabled()) {
            return;
        }
        String email = normalizeEmail(user.getEmail());
        boolean allowlisted = email != null && admin.getEmailAllowlist()
                .stream()
                .map(LoginService::normalizeEmail)
                .anyMatch(email::equals);
        if (!allowlisted) {
            return;
        }
        TenantEntity institution = tenantRepository.save(
                TenantEntity.institution(admin.getInstitutionName(),
                        admin.getTimeZone()));
        membershipRepository.save(
                MembershipEntity.create(institution, user,
                        MembershipRole.INSTITUTION_ADMIN));
    }

    private TenantId firstActiveInstitution(UUID userId) {
        return membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)
                .stream()
                .filter(LoginService::tenantActive)
                .map(m -> new TenantId(m.getTenant().getId()))
                .findFirst()
                .orElse(null);
    }

    private static boolean tenantActive(MembershipEntity m) {
        return m.getTenant().getStatus() == TenantStatus.ACTIVE;
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}