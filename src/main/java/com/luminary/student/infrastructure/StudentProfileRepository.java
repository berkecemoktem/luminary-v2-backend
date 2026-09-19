package com.luminary.student.infrastructure;

import com.luminary.student.domain.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfileEntity, UUID> {

    Optional<StudentProfileEntity> findByTenantIdAndUserId(String tenantId,
                                                           UUID userId);

    List<StudentProfileEntity> findAllByTenantId(String tenantId);
}
