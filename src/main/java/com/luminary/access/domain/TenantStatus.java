package com.luminary.access.domain;

/**
 * Record status of a tenant. Disabled tenants deny new work and valid
 * membership checks against them.
 */
public enum TenantStatus {
    ACTIVE,
    DISABLED
}