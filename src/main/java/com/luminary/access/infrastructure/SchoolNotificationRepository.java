package com.luminary.access.infrastructure;

import com.luminary.access.domain.SchoolNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SchoolNotificationRepository
        extends JpaRepository<SchoolNotificationEntity, UUID> {

    List<SchoolNotificationEntity>
            findByTenantIdOrderByCreatedAtDesc(String tenantId);

    long countByTenantIdAndReadAtIsNull(String tenantId);
}