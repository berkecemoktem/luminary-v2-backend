package com.luminary.student.application;

import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import com.luminary.shared.port.MembershipVerifier;
import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;
import com.luminary.student.domain.StudentProfileEntity;
import com.luminary.student.infrastructure.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the tenant-scoped student profile. A student may only
 * manage their own profile; an institution admin may view any profile
 * within their tenant.
 */
@Service
public class StudentProfileService {

    private final StudentProfileRepository profileRepository;
    private final MembershipVerifier membershipVerifier;

    public StudentProfileService(
            StudentProfileRepository profileRepository,
            MembershipVerifier membershipVerifier) {
        this.profileRepository = profileRepository;
        this.membershipVerifier = membershipVerifier;
    }

    /**
     * Returns the current student's profile in the active tenant.
     * Creates an empty profile if none exists yet (first-time onboarding).
     */
    @Transactional
    public StudentProfileView getOwnProfile(UserId userId,
                                            TenantId tenantId) {
        membershipVerifier.requireActiveMembership(
                tenantId.value(), userId.value().toString());

        StudentProfileEntity profile = profileRepository
                .findByTenantIdAndUserId(tenantId.value(), userId.value())
                .orElseGet(() -> profileRepository.save(
                        StudentProfileEntity.create(
                                tenantId.value(), userId.value(),
                                GradeLevel.GRADE_12,
                                StudentField.SAYISAL,
                                null)));

        return toView(profile);
    }

    /**
     * Updates the current student's profile. Only the student themselves
     * may update; institution admins do not edit student profiles through
     * this endpoint.
     */
    @Transactional
    public StudentProfileView updateOwnProfile(UserId userId,
                                               TenantId tenantId,
                                               UpdateStudentProfileRequest request) {
        membershipVerifier.requireActiveMembership(
                tenantId.value(), userId.value().toString());

        StudentProfileEntity profile = profileRepository
                .findByTenantIdAndUserId(tenantId.value(), userId.value())
                .orElseGet(() -> profileRepository.save(
                        StudentProfileEntity.create(
                                tenantId.value(), userId.value(),
                                request.gradeLevel(),
                                request.field(),
                                request.schoolName())));

        if (request.gradeLevel() != null) {
            profile.setGradeLevel(request.gradeLevel());
        }
        if (request.field() != null) {
            profile.setField(request.field());
        }
        if (request.schoolName() != null) {
            profile.setSchoolName(request.schoolName());
        }
        profile.markUpdated();

        return toView(profileRepository.save(profile));
    }

    /**
     * Returns a student's profile as viewed by an institution admin.
     * The student must be a member of the same tenant.
     */
    @Transactional(readOnly = true)
    public StudentProfileView getStudentProfile(UserId actorUserId,
                                                TenantId tenantId,
                                                UserId targetUserId) {
        String actorRole = membershipVerifier.requireActiveRole(
                tenantId.value(), actorUserId.value().toString());
        if (!membershipVerifier.isInstitutionAdmin(actorRole)) {
            throw ApiException.forbidden("role-required",
                    "Only an institution admin can view other student profiles.");
        }

        membershipVerifier.requireActiveMembership(
                tenantId.value(), targetUserId.value().toString());

        StudentProfileEntity profile = profileRepository
                .findByTenantIdAndUserId(tenantId.value(), targetUserId.value())
                .orElseThrow(() -> ApiException.notFound(
                        "profile-not-found",
                        "No profile found for this student."));

        return toView(profile);
    }

    private static StudentProfileView toView(StudentProfileEntity profile) {
        return new StudentProfileView(
                profile.getId().toString(),
                profile.getTenantId(),
                profile.getUserId().toString(),
                profile.getGradeLevel(),
                profile.getField(),
                profile.getSchoolName());
    }
}
