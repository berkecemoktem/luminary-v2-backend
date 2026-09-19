package com.luminary.access.infrastructure.adapter;

import com.luminary.access.domain.MembershipEntity;
import com.luminary.access.domain.MembershipRole;
import com.luminary.access.domain.MembershipStatus;
import com.luminary.access.infrastructure.MembershipRepository;
import com.luminary.shared.port.StudentDirectoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter exposing the access-module membership aggregate to the student
 * directory through {@link StudentDirectoryPort}.
 */
@Component
public class StudentDirectoryAdapter implements StudentDirectoryPort {

    private final MembershipRepository membershipRepository;

    public StudentDirectoryAdapter(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public List<Member> members(String tenantId) {
        return membershipRepository
                .findByTenantIdAndRoleAndStatus(
                        tenantId, MembershipRole.STUDENT,
                        MembershipStatus.ACTIVE)
                .stream()
                .sorted((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()))
                .map(this::toMember)
                .toList();
    }

    @Override
    public Optional<Member> member(String tenantId, UUID userId) {
        return membershipRepository
                .findByTenantIdAndUserId(tenantId, userId)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .filter(m -> m.getRole() == MembershipRole.STUDENT)
                .map(this::toMember);
    }

    private Member toMember(MembershipEntity membership) {
        var user = membership.getUser();
        return new Member(
                user.getId(), user.getDisplayName(), user.getEmail(),
                user.getCountry(), user.getCity(), membership.getJoinedAt());
    }
}