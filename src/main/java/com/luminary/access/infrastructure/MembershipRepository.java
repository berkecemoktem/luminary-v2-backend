package com.luminary.access.infrastructure;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository
        extends JpaRepository<MembershipEntity, UUID> {

    List<MembershipEntity> findByUserId(UUID userId);

    Optional<MembershipEntity> findByTenantIdAndUserId(String tenantId,
                                                       UUID userId);

    List<MembershipEntity> findByUserIdAndStatus(UUID userId,
                                                 MembershipStatus status);

    boolean existsByTenantIdAndUserId(String tenantId, UUID userId);
}