package com.luminary.access.application;

import com.luminary.access.application.port.CurrentSession;
import com.luminary.access.domain.AuthIdentity;
import com.luminary.access.domain.InvitationEntity;
import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.domain.TenantEntity;
import com.luminary.access.domain.TenantType;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.InvitationRepository;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationServiceTest {

    private InvitationRepository invitationRepository;
    private MembershipRepository membershipRepository;
    private UserRepository userRepository;
    private CurrentSession currentSession;
    private AccessAppProperties properties;
    private InvitationService service;

    private UserEntity admin;
    private UserEntity student;
    private TenantEntity institution;
    private MembershipEntity adminMembership;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(InvitationRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        userRepository = mock(UserRepository.class);
        currentSession = mock(CurrentSession.class);
        properties = new AccessAppProperties();
        service = new InvitationService(invitationRepository,
                membershipRepository, userRepository, currentSession,
                properties);

        admin = UserEntity.create(
                new AuthIdentity("iss", "sub-admin"),
                "admin@luminary.dev", "Admin");
        student = UserEntity.create(
                new AuthIdentity("iss", "sub-student"),
                "student@luminary.dev", "Student");
        institution = TenantEntity.institution("Luminary Demo", "UTC");
        adminMembership = MembershipEntity.create(institution, admin,
                MembershipRole.INSTITUTION_ADMIN);
    }

    @Test
    void create_returnsSingleUseTokenAndStoresHash() {
        when(membershipRepository.findByTenantIdAndUserId(
                institution.getId(), admin.getId()))
                .thenReturn(Optional.of(adminMembership));
        when(invitationRepository
                .findByTenantIdAndEmailAndUsedAtIsNull(
                        institution.getId(), "newuser@luminary.dev"))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(InvitationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.save(any(MembershipEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InvitationCreated result = service.create(admin.getId(),
                new TenantId(institution.getId()),
                "  NewUser@Luminary.Dev ", null);

        assertThat(result.email()).isEqualTo("newuser@luminary.dev");
        assertThat(result.role()).isEqualTo(MembershipRole.STUDENT);
        assertThat(result.token()).isNotEmpty();
        assertThat(result.expiresAt())
                .isAfter(OffsetDateTime.now().plusDays(6).plusHours(23));

        verify(invitationRepository).save(argThat(inv -> inv.getTokenHash()
                .equals(sha256(result.token()))));
        assertThat(result.acceptUrl()).contains(result.token());
    }

    @Test
    void create_rejectsNonAdminActor() {
        MembershipEntity studentMembership = MembershipEntity.create(
                institution, student, MembershipRole.STUDENT);
        when(membershipRepository.findByTenantIdAndUserId(
                institution.getId(), student.getId()))
                .thenReturn(Optional.of(studentMembership));

        assertThatThrownBy(() -> service.create(student.getId(),
                new TenantId(institution.getId()),
                "x@luminary.dev", MembershipRole.STUDENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("role-required-institution-admin"));
    }

    @Test
    void accept_createsMembershipAndMarksInvitationUsed() {
        InvitationEntity invitation = pendingInvitation();
        when(invitationRepository.findByTokenHash(sha256("raw-token")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(membershipRepository.existsByTenantIdAndUserId(
                institution.getId(), student.getId()))
                .thenReturn(false);
        when(membershipRepository.findByUserIdAndStatus(student.getId(),
                MembershipStatus.ACTIVE)).thenReturn(List.of());
        when(membershipRepository.save(any(MembershipEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InvitedWorkspace result = service.accept(student.getId(),
                "raw-token");

        assertThat(result.tenantId()).isEqualTo(
                new TenantId(institution.getId()));
        assertThat(result.role()).isEqualTo(MembershipRole.STUDENT);
        assertThat(result.joinedAt()).isNotNull();
    }

    @Test
    void accept_usedInvitation_isRejected() {
        InvitationEntity invitation = pendingInvitation();
        invitation.markUsed(OffsetDateTime.now().minusMinutes(1));
        when(invitationRepository.findByTokenHash(sha256("raw-token")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.accept(student.getId(),
                "raw-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("invitation-already-used"));
    }

    @Test
    void accept_expiredInvitation_isRejected() {
        InvitationEntity invitation = InvitationEntity.create(institution,
                "student@luminary.dev", MembershipRole.STUDENT,
                sha256("s"), OffsetDateTime.now().minusMinutes(1),
                admin.getId());
        when(invitationRepository.findByTokenHash(sha256("s")))
                .thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.accept(student.getId(), "s"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("invitation-expired"));
    }

    @Test
    void accept_wrongAccount_isRejected() {
        InvitationEntity invitation = pendingInvitation();
        when(invitationRepository.findByTokenHash(sha256("raw-token")))
                .thenReturn(Optional.of(invitation));
        UserEntity other = UserEntity.create(
                new AuthIdentity("iss", "sub-other"),
                "someone-else@luminary.dev", "Other");
        when(userRepository.findById(other.getId()))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.accept(other.getId(),
                "raw-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("invitation-account-mismatch"));
    }

    @Test
    void accept_withExistingActiveInstitution_isRejected() {
        InvitationEntity invitation = pendingInvitation();
        when(invitationRepository.findByTokenHash(sha256("raw-token")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(membershipRepository.existsByTenantIdAndUserId(
                institution.getId(), student.getId()))
                .thenReturn(false);
        MembershipEntity otherInstitution = MembershipEntity.create(
                TenantEntity.institution("Other School", "UTC"), student,
                MembershipRole.STUDENT);
        when(membershipRepository.findByUserIdAndStatus(student.getId(),
                MembershipStatus.ACTIVE))
                .thenReturn(List.of(otherInstitution));

        assertThatThrownBy(() -> service.accept(student.getId(),
                "raw-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("active-institution-exists"));
    }

    @Test
    void accept_alreadyMember_isRejected() {
        InvitationEntity invitation = pendingInvitation();
        when(invitationRepository.findByTokenHash(sha256("raw-token")))
                .thenReturn(Optional.of(invitation));
        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(membershipRepository.existsByTenantIdAndUserId(
                institution.getId(), student.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.accept(student.getId(),
                "raw-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(
                        ((ApiException) e).getCode())
                        .isEqualTo("already-member"));
    }

    private InvitationEntity pendingInvitation() {
        return InvitationEntity.create(institution,
                "student@luminary.dev", MembershipRole.STUDENT,
                sha256("raw-token"),
                OffsetDateTime.now().plusDays(7), admin.getId());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}