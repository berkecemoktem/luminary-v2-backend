package com.luminary.student.application;

import com.luminary.shared.port.StudentDirectoryPort;
import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;
import com.luminary.student.domain.StudentProfileEntity;

import java.time.OffsetDateTime;

/**
 * Full student record shown on the institution's student-details page.
 */
public record StudentDetailsView(String userId, String displayName,
                                 String email, String country, String city,
                                 GradeLevel gradeLevel, StudentField field,
                                 String schoolName,
                                 OffsetDateTime joinedAt,
                                 OffsetDateTime profileUpdatedAt) {

    public static StudentDetailsView from(StudentDirectoryPort.Member member,
                                          StudentProfileEntity profile) {
        return new StudentDetailsView(
                member.userId().toString(),
                member.displayName(),
                member.email(),
                member.country(),
                member.city(),
                profile == null ? null : profile.getGradeLevel(),
                profile == null ? null : profile.getField(),
                profile == null ? null : profile.getSchoolName(),
                member.joinedAt(),
                profile == null ? null : profile.getUpdatedAt());
    }
}