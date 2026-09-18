package com.luminary.student.application;

import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import com.luminary.shared.port.MembershipVerifier;
import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;
import com.luminary.student.domain.StudentProfileEntity;
import com.luminary.student.infrastructure.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentProfileServiceTest {

    private StudentProfileRepository profileRepository;
    private MembershipVerifier membershipVerifier;
    private StudentProfileService service;

    private UUID studentId;
    private UUID adminId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        profileRepository = mock(StudentProfileRepository.class);
        membershipVerifier = mock(MembershipVerifier.class);
        service = new StudentProfileService(profileRepository,
                membershipVerifier);

        studentId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        tenantId = "test-tenant";
    }

    @Test
    void getOwnProfile_createsDefault_whenNoProfileExists() {
        when(membershipVerifier.requireActiveRole(tenantId,
                studentId.toString())).thenReturn("STUDENT");

        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentProfileView result = service.getOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId));

        assertThat(result.gradeLevel()).isEqualTo(GradeLevel.GRADE_12);
        assertThat(result.field()).isEqualTo(StudentField.SAYISAL);
        assertThat(result.schoolName()).isNull();

        ArgumentCaptor<StudentProfileEntity> captor =
                ArgumentCaptor.forClass(StudentProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(studentId);
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void getOwnProfile_returnsExistingProfile() {
        when(membershipVerifier.requireActiveRole(tenantId,
                studentId.toString())).thenReturn("STUDENT");

        StudentProfileEntity existing = StudentProfileEntity.create(
                tenantId, studentId,
                GradeLevel.GRADE_11, StudentField.SOZEL, "Ankara Lisesi");
        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.of(existing));

        StudentProfileView result = service.getOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId));

        assertThat(result.gradeLevel()).isEqualTo(GradeLevel.GRADE_11);
        assertThat(result.field()).isEqualTo(StudentField.SOZEL);
        assertThat(result.schoolName()).isEqualTo("Ankara Lisesi");
    }

    @Test
    void updateOwnProfile_createsProfile_whenNoProfileExists() {
        when(membershipVerifier.requireActiveRole(tenantId,
                studentId.toString())).thenReturn("STUDENT");

        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.empty());
        when(profileRepository.save(any(StudentProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateStudentProfileRequest request =
                new UpdateStudentProfileRequest(
                        GradeLevel.GRADE_11, StudentField.DIL,
                        "Istanbul Lisesi");

        StudentProfileView result = service.updateOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId), request);

        assertThat(result.gradeLevel()).isEqualTo(GradeLevel.GRADE_11);
        assertThat(result.field()).isEqualTo(StudentField.DIL);
        assertThat(result.schoolName()).isEqualTo("Istanbul Lisesi");
    }

    @Test
    void updateOwnProfile_updatesExistingFields() {
        when(membershipVerifier.requireActiveRole(tenantId,
                studentId.toString())).thenReturn("STUDENT");

        StudentProfileEntity existing = StudentProfileEntity.create(
                tenantId, studentId,
                GradeLevel.GRADE_12, StudentField.SAYISAL, "Old School");
        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.of(existing));
        when(profileRepository.save(any(StudentProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateStudentProfileRequest request =
                new UpdateStudentProfileRequest(
                        GradeLevel.GRADE_11, null, "New School");

        StudentProfileView result = service.updateOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId), request);

        assertThat(result.gradeLevel()).isEqualTo(GradeLevel.GRADE_11);
        assertThat(result.field()).isEqualTo(StudentField.SAYISAL);
        assertThat(result.schoolName()).isEqualTo("New School");
    }

    @Test
    void getStudentProfile_adminCanViewStudentInSameTenant() {
        stubAdmin();

        StudentProfileEntity profile = StudentProfileEntity.create(
                tenantId, studentId,
                GradeLevel.GRADE_12, StudentField.ESIT_AGIRLIK, null);
        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.of(profile));

        StudentProfileView result = service.getStudentProfile(
                new UserId(adminId),
                new TenantId(tenantId),
                new UserId(studentId));

        assertThat(result.gradeLevel()).isEqualTo(GradeLevel.GRADE_12);
        assertThat(result.field()).isEqualTo(StudentField.ESIT_AGIRLIK);
    }

    @Test
    void getStudentProfile_rejectsStudentViewingOtherStudent() {
        when(membershipVerifier.requireActiveRole(tenantId,
                studentId.toString())).thenReturn("STUDENT");
        when(membershipVerifier.isInstitutionAdmin("STUDENT"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getStudentProfile(
                new UserId(studentId),
                new TenantId(tenantId),
                new UserId(UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo("role-required"));
    }

    @Test
    void getStudentProfile_rejectsAdmin_forStudentInOtherTenant() {
        stubAdmin();

        UUID otherStudentId = UUID.randomUUID();
        doThrow(ApiException.forbidden("membership-not-found",
                "Not a member."))
                .when(membershipVerifier)
                .requireActiveMembership(tenantId, otherStudentId.toString());

        assertThatThrownBy(() -> service.getStudentProfile(
                new UserId(adminId),
                new TenantId(tenantId),
                new UserId(otherStudentId)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo("membership-not-found"));
    }

    @Test
    void getOwnProfile_rejectsRevokedMembership() {
        doThrow(ApiException.forbidden("membership-revoked",
                "Revoked."))
                .when(membershipVerifier)
                .requireActiveMembership(tenantId, studentId.toString());

        assertThatThrownBy(() -> service.getOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo("membership-revoked"));
    }

    @Test
    void getOwnProfile_rejectsInactiveTenant() {
        doThrow(ApiException.forbidden("tenant-inactive",
                "Inactive."))
                .when(membershipVerifier)
                .requireActiveMembership(tenantId, studentId.toString());

        assertThatThrownBy(() -> service.getOwnProfile(
                new UserId(studentId),
                new TenantId(tenantId)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo("tenant-inactive"));
    }

    @Test
    void getStudentProfile_profileNotFound_throwsNotFound() {
        stubAdmin();

        when(profileRepository.findByTenantIdAndUserId(tenantId, studentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentProfile(
                new UserId(adminId),
                new TenantId(tenantId),
                new UserId(studentId)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode())
                        .isEqualTo("profile-not-found"));
    }

    private void stubAdmin() {
        when(membershipVerifier.requireActiveRole(tenantId,
                adminId.toString())).thenReturn("INSTITUTION_ADMIN");
        when(membershipVerifier.isInstitutionAdmin("INSTITUTION_ADMIN"))
                .thenReturn(true);
    }
}
