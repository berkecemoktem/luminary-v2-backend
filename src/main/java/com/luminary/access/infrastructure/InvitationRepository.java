package com.luminary.access.infrastructure;

import com.luminary.access.domain.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository
        extends JpaRepository<InvitationEntity, UUID> {

    Optional<InvitationEntity> findByTokenHash(String tokenHash);

    Optional<InvitationEntity> findByTenantIdAndEmailAndUsedAtIsNull(
            String tenantId, String email);

    Optional<InvitationEntity> findFirstByTenantIdAndEmailOrderByCreatedAtDesc(
            String tenantId, String email);
}