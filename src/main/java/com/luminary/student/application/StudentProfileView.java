package com.luminary.student.application;

import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;

/**
 * Read view of a student profile. Returned by all profile APIs.
 */
public record StudentProfileView(String profileId, String tenantId,
                                 String userId, GradeLevel gradeLevel,
                                 StudentField field, String schoolName) {
}
