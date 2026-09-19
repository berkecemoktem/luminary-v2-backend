package com.luminary.student.application;

import com.luminary.shared.port.StudentDirectoryPort;
import com.luminary.student.domain.GradeLevel;
import com.luminary.student.domain.StudentField;
import com.luminary.student.domain.StudentProfileEntity;

import java.time.OffsetDateTime;

/**
 * Row of the institution's student directory table.
 */
public record StudentListItemView(String userId, String displayName,
                                  String email, String country, String city,
                                  GradeLevel gradeLevel, StudentField field,
                                  String schoolName,
                                  OffsetDateTime joinedAt) {

    public static StudentListItemView from(StudentDirectoryPort.Member member,
                                           StudentProfileEntity profile) {
        return new StudentListItemView(
                member.userId().toString(),
                member.displayName(),
                member.email(),
                member.country(),
                member.city(),
                profile == null ? null : profile.getGradeLevel(),
                profile == null ? null : profile.getField(),
                profile == null ? null : profile.getSchoolName(),
                member.joinedAt());
    }
}