package com.luminary.access.infrastructure;

import com.luminary.access.domain.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
}