package com.luminary.student.api;

import com.luminary.access.api.CurrentUser;
import com.luminary.access.api.dto.MeResponse;
import com.luminary.access.domain.UserEntity;
import com.luminary.access.infrastructure.UserRepository;
import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.UserId;
import com.luminary.student.api.dto.StudentProfileResponse;
import com.luminary.student.application.StudentProfileService;
import com.luminary.student.application.StudentProfileView;
import com.luminary.student.application.UpdateStudentProfileRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student profile endpoints. A student manages their own profile;
 * an institution admin may view any profile within their tenant.
 */
@RestController
@RequestMapping("/api/v1")
public class StudentProfileController {

    private final CurrentUser currentUser;
    private final StudentProfileService profileService;

    public StudentProfileController(CurrentUser currentUser,
                                    StudentProfileService profileService) {
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    @GetMapping("/students/me")
    public StudentProfileResponse getOwnProfile() {
        UserId userId = currentUser.require();
        return toResponse(profileService.getOwnProfile(
                userId, currentUser.requireTenant()));
    }

    @PatchMapping("/students/me")
    public StudentProfileResponse updateOwnProfile(
            @RequestBody UpdateStudentProfileRequest body) {
        UserId userId = currentUser.require();
        return toResponse(profileService.updateOwnProfile(
                userId, currentUser.requireTenant(), body));
    }

    @GetMapping("/institution/students/{userId}")
    public StudentProfileResponse getStudentProfile(
            @PathVariable String userId) {
        UserId actorId = currentUser.require();
        UserId targetId = UserId.fromString(userId);
        return toResponse(profileService.getStudentProfile(
                actorId, currentUser.requireTenant(), targetId));
    }

    private static StudentProfileResponse toResponse(StudentProfileView view) {
        return new StudentProfileResponse(
                view.profileId(), view.tenantId(), view.userId(),
                view.gradeLevel(), view.field(), view.schoolName());
    }
}
