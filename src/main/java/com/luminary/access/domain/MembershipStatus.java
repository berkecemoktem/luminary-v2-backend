package com.luminary.access.domain;

import java.time.Instant;

/**
 * Runtime state of a membership. Revoked membership is rejected immediately
 * on every protected call (E03-05).
 */
public enum MembershipStatus {
    ACTIVE,
    REVOKED
}