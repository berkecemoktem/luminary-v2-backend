package com.luminary.access.domain;

/**
 * Tenant scope: {@code INSTITUTION} for organizational workspaces (courtesy of
 * the pilot institutions) or {@code PERSONAL} for the student's own workspace.
 * Personal tenants are created automatically on first B2C login.
 */
public enum TenantType {
    INSTITUTION,
    PERSONAL
}