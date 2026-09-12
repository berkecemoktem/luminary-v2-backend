package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.AuthIdentity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.TenantType;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

    private MembershipRepository membershipRepository;
    private CurrentSession currentSession;
    private WorkspaceService service;

    private UserEntity user;
    private TenantEntity personal;
    private TenantEntity institution;
    private MembershipEntity personalMembership;
    private MembershipEntity institutionMembership;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipRepository.class);
        currentSession = mock(CurrentSession.class);
        service = new WorkspaceService(membershipRepository,
                currentSession);
        user = UserEntity.create(
                new AuthIdentity("https://idp.test/luminary", "sub-1"),
                "s@luminary.dev", "Student");
        personal = TenantEntity.personal("Student");
        institution = TenantEntity.institution("Luminary Demo", "UTC");
        personalMembership = MembershipEntity.create(personal, user,
                MembershipRole.STUDENT);
        institutionMembership = MembershipEntity.create(institution, user,
                MembershipRole.INSTITUTION_ADMIN);
    }

    @Test
    void workspaces_listsOnlyActiveMembershipsWithActiveTenants() {
        var revoked = MembershipEntity.create(institution, user,
                MembershipRole.STUDENT);
        revoked.revoke();
        when(membershipRepository.findByUserId(user.getId()))
                .thenReturn(List.of(personalMembership,
                        institutionMembership, revoked));

        List<WorkspaceView> views = service.workspaces(
                new UserId(user.getId()));

        assertThat(views).hasSize(2);
        assertThat(views).extracting(WorkspaceView::tenantId)
                .containsExactly(new TenantId(personal.getId()),
                        new TenantId(institution.getId()));
    }

    @Test
    void current_usesValidCachedTenant() {
        when(membershipRepository.findByTenantIdAndUserId(
                personal.getId(), user.getId()))
                .thenReturn(Optional.of(personalMembership));
        when(currentSession.activeTenant())
                .thenReturn(Optional.of(new TenantId(personal.getId())));

        TenantId result = service.current(new UserId(user.getId()));

        assertThat(result).isEqualTo(new TenantId(personal.getId()));
        verify(currentSession, never()).authenticate(any(UserId.class),
                any(TenantId.class));
    }

    @Test
    void current_recomputesDefaultWhenCachedIsStale() {
        when(currentSession.activeTenant()).thenReturn(Optional.empty());
        when(membershipRepository.findByUserIdAndStatus(user.getId(),
                MembershipStatus.ACTIVE))
                .thenReturn(List.of(personalMembership,
                        institutionMembership));

        TenantId result = service.current(new UserId(user.getId()));

        assertThat(result).isEqualTo(new TenantId(
                institutionMembership.getTenant().getId()));
        verify(currentSession).authenticate(
                new UserId(user.getId()),
                new TenantId(institution.getId()));
    }

    @Test
    void current_withNoActiveWorkspace_throws() {
        when(currentSession.activeTenant()).thenReturn(Optional.empty());
        when(membershipRepository.findByUserIdAndStatus(user.getId(),
                MembershipStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> service.current(new UserId(user.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("no-active-workspace"));
    }

    @Test
    void switchTo_revokedMembership_isRejectedImmediately() {
        var revoked = MembershipEntity.create(institution, user,
                MembershipRole.STUDENT);
        revoked.revoke();
        when(membershipRepository.findByTenantIdAndUserId(
                institution.getId(), user.getId()))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.switchTo(
                new UserId(user.getId()),
                new TenantId(institution.getId())))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("membership-revoked"));
    }

    @Test
    void switchTo_happyPath_persistsNewActiveTenant() {
        when(membershipRepository.findByTenantIdAndUserId(
                institution.getId(), user.getId()))
                .thenReturn(Optional.of(institutionMembership));

        WorkspaceView view = service.switchTo(
                new UserId(user.getId()),
                new TenantId(institution.getId()));

        assertThat(view.tenantId()).isEqualTo(
                new TenantId(institution.getId()));
        assertThat(view.role()).isEqualTo(MembershipRole.INSTITUTION_ADMIN);
        verify(currentSession).authenticate(
                new UserId(user.getId()),
                new TenantId(institution.getId()));
    }
}