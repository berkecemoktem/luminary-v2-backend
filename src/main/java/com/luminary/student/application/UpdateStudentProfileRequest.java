package com.luminary.student.application;

import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;

/**
 * Request body for creating or updating a student profile.
 */
public record UpdateStudentProfileRequest(GradeLevel gradeLevel,
                                          StudentField field,
                                          String schoolName) {
}
