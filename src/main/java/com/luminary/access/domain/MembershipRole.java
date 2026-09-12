package com.luminary.access.domain;

/**
 * Membership roles in the MVP. A separate teacher/guidance role is out of
 * MVP scope; staff users in pilot institutions operate as INSTITUTION_ADMIN.
 */
public enum MembershipRole {
    STUDENT,
    INSTITUTION_ADMIN
}