package com.luminary.access.infrastructure;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository
        extends JpaRepository<MembershipEntity, UUID>,
        JpaSpecificationExecutor<MembershipEntity> {

    @Override
    @EntityGraph(attributePaths = {"tenant"})
    Page<MembershipEntity> findAll(Specification<MembershipEntity> spec,
                                   Pageable pageable);

    List<MembershipEntity> findByUserId(UUID userId);

    Optional<MembershipEntity> findByTenantIdAndUserId(String tenantId,
                                                       UUID userId);

    List<MembershipEntity> findByUserIdAndStatus(UUID userId,
                                                 MembershipStatus status);

    boolean existsByTenantIdAndUserId(String tenantId, UUID userId);
}
