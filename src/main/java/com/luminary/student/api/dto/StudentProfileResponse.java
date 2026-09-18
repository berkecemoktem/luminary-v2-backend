package com.luminary.student.api.dto;

import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;

/**
 * Response body for student profile endpoints.
 */
public record StudentProfileResponse(String profileId, String tenantId,
                                     String userId, GradeLevel gradeLevel,
                                     StudentField field, String schoolName) {
}
