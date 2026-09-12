package com.luminary.access.domain;

/**
 * Record status of a user account. Disabled accounts cannot authenticate;
 * an ACTIVE membership belonging to a DISABLED user is still stored but
 * effectively denied at login.
 */
public enum AccountStatus {
    ACTIVE,
    DISABLED
}