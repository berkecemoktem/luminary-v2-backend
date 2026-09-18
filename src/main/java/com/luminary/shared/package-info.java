/**
 * Shared foundation package for the Luminary backend.
 *
 * <p>Contains only genuinely common concepts that all domain modules may rely on:
 * identity types, time/clock utilities, the error model (RFC 9457 Problem
 * Details), versioned event envelopes and the common list-query contract. It
 * must never import any domain module and must not grow into a general-purpose
 * utilities package.</p>
 */
package com.luminary.shared;
