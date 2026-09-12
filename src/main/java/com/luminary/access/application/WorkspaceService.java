package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantStatus;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final MembershipRepository membershipRepository;
    private final CurrentSession currentSession;

    public WorkspaceService(MembershipRepository membershipRepository,
                            CurrentSession currentSession) {
        this.membershipRepository = membershipRepository;
        this.currentSession = currentSession;
    }

    /**
     * Read view (E03-07). Only workspaces the caller is actively a member of,
     * whose tenant is also active, are shown.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceView> workspaces(UserId userId) {
        return membershipRepository.findByUserId(userId.value()).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE
                        && m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .map(WorkspaceView::from)
                .toList();
    }

    /**
     * Returns the effective active tenant for this session, validating it is
     * still live. If the session carries an invalid tenant, or is empty,
     * recomputes a valid default and persists it. Throws only when the
     * account has no active workspace at all.
     */
    @Transactional(readOnly = true)
    public TenantId current(UserId userId) {
        return currentOrEmpty(userId).orElseThrow(() -> ApiException.forbidden(
                "no-active-workspace",
                "No active workspace for this account. "
                        + "Contact your administrator."));
    }

    /**
     * Same as {@link #current(UserId)} but returns {@link Optional#empty()}
     * for tenantless accounts instead of throwing. Used by {@code GET /me}.
     */
    @Transactional(readOnly = true)
    public Optional<TenantId> currentOrEmpty(UserId userId) {
        TenantId cached = currentSession.activeTenant().orElse(null);
        if (cached != null && isActiveMembership(userId, cached)) {
            return Optional.of(cached);
        }

        return membershipRepository
                .findByUserIdAndStatus(userId.value(),
                        MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .sorted((a, b) -> Boolean.compare(
                        a.getTenant().getType()
                                == com.luminary.access.domain.TenantType.PERSONAL,
                        b.getTenant().getType()
                                == com.luminary.access.domain.TenantType.PERSONAL))
                .map(m -> new TenantId(m.getTenant().getId()))
                .findFirst()
                .map(TenantId -> {
                    currentSession.authenticate(userId, TenantId);
                    return TenantId;
                });
    }

    @Transactional
    public WorkspaceView switchTo(UserId userId, TenantId target) {
        MembershipEntity membership = membershipRepository
                .findByTenantIdAndUserId(target.value(), userId.value())
                .orElseThrow(() -> ApiException.notFound(
                        "membership-not-found",
                        "You are not a member of this workspace."));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.forbidden(
                    "membership-revoked",
                    "Your membership in this workspace has been revoked.");
        }
        if (membership.getTenant().getStatus() != TenantStatus.ACTIVE) {
            throw ApiException.forbidden(
                    "workspace-inactive",
                    "This workspace is inactive.");
        }

        currentSession.authenticate(userId, target);
        return WorkspaceView.from(membership);
    }

    private boolean isActiveMembership(UserId userId, TenantId tenantId) {
        return membershipRepository
                .findByTenantIdAndUserId(tenantId.value(), userId.value())
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE
                        && m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .isPresent();
    }
}