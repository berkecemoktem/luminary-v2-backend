package com.luminary.student.application;

import com.luminary.shared.error.ApiException;
import com.luminary.shared.identity.TenantId;
import com.luminary.shared.identity.UserId;
import com.luminary.shared.port.MembershipVerifier;
import com.luminary.shared.query.FilterCriterion;
import com.luminary.shared.query.ListQuery;
import com.luminary.shared.query.Page;
import com.luminary.shared.query.SortCriterion;
import com.luminary.shared.port.StudentDirectoryPort;
import com.luminary.shared.port.StudentDirectoryPort.Member;
import com.luminary.student.domain.StudentProfileEntity;
import com.luminary.student.infrastructure.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Students directory for institution admins: a paged, searchable/sortable
 * table of the tenant's students plus a single-student details view. Editing
 * remains student-self-serve through {@link StudentProfileService}.
 */
@Service
public class StudentDirectoryService {

    private static final int MAX_TAKE = 200;

    private final StudentDirectoryPort directoryPort;
    private final StudentProfileRepository profileRepository;
    private final MembershipVerifier membershipVerifier;

    public StudentDirectoryService(StudentDirectoryPort directoryPort,
                                   StudentProfileRepository profileRepository,
                                   MembershipVerifier membershipVerifier) {
        this.directoryPort = directoryPort;
        this.profileRepository = profileRepository;
        this.membershipVerifier = membershipVerifier;
    }

    @Transactional(readOnly = true)
    public Page<StudentListItemView> list(UUID actorUserId,
                                          TenantId tenantId,
                                          ListQuery query) {
        requireInstitutionAdmin(actorUserId, tenantId);

        int take = Math.min(Math.max(1, query.takeOrDefault()), MAX_TAKE);
        int skip = query.skipOrDefault();

        Map<UUID, StudentProfileEntity> profilesByUser =
                profileRepository.findAllByTenantId(tenantId.value())
                        .stream()
                        .collect(Collectors.toMap(
                                StudentProfileEntity::getUserId,
                                Function.identity(), (a, b) -> a));

        List<StudentListItemView> all = directoryPort.members(tenantId.value())
                .stream()
                .map(member -> StudentListItemView.from(member,
                        profilesByUser.get(member.userId())))
                .filter(item -> matchesAll(item, query.filtersOrEmpty()))
                .sorted(comparator(query.sortsOrEmpty()))
                .toList();

        return paginate(all, skip, take);
    }

    @Transactional(readOnly = true)
    public StudentDetailsView details(UUID actorUserId, TenantId tenantId,
                                      UserId targetUserId) {
        requireInstitutionAdmin(actorUserId, tenantId);

        Member member = directoryPort
                .member(tenantId.value(), targetUserId.value())
                .orElseThrow(() -> ApiException.notFound(
                        "student-not-found",
                        "No active student found for this account in this "
                                + "workspace."));
        StudentProfileEntity profile = profileRepository
                .findByTenantIdAndUserId(tenantId.value(),
                        targetUserId.value())
                .orElse(null);

        return StudentDetailsView.from(member, profile);
    }

    private void requireInstitutionAdmin(UUID actorUserId,
                                         TenantId tenantId) {
        String role = membershipVerifier.requireActiveRole(
                tenantId.value(), actorUserId.toString());
        if (!membershipVerifier.isInstitutionAdmin(role)) {
            throw ApiException.forbidden("role-required",
                    "Only an institution admin can access the student "
                            + "directory.");
        }
    }

    private static Page<StudentListItemView> paginate(
            List<StudentListItemView> all, int skip, int take) {
        int from = Math.min(skip, all.size());
        int to = Math.min(skip + take, all.size());
        List<StudentListItemView> items = all.subList(from, to);
        return new Page<>(items, skip, take, all.size(), to < all.size());
    }

    private static boolean matchesAll(StudentListItemView item,
                                      List<FilterCriterion> filters) {
        return filters.stream().allMatch(f -> matches(item, f));
    }

    private static boolean matches(StudentListItemView item,
                                   FilterCriterion filter) {
        String field = filter.field() == null ? "" : filter.field();
        String operator = filter.operator() == null ? "" : filter.operator();
        String value = filter.value() == null ? "" : filter.value().toString();
        String actual = switch (field) {
            case "name" -> item.displayName();
            case "email" -> item.email();
            case "city" -> item.city();
            case "country" -> item.country();
            default -> null;
        };
        if (actual == null) {
            return false;
        }
        String a = actual.toUpperCase(Locale.ROOT);
        String v = value.toUpperCase(Locale.ROOT);
        return switch (operator) {
            case "STARTS_WITH" -> a.startsWith(v);
            case "EQUALS" -> a.equals(v);
            default -> a.contains(v);
        };
    }

    private static Comparator<StudentListItemView> comparator(
            List<SortCriterion> sorts) {
        List<SortCriterion> criteria = sorts == null || sorts.isEmpty()
                ? List.of(new SortCriterion("name", "ASC"))
                : sorts;
        Comparator<StudentListItemView> result = Comparator
                .comparing(StudentListItemView::displayName,
                        Comparator.nullsLast(String::compareToIgnoreCase));
        for (SortCriterion sort : criteria) {
            Comparator<StudentListItemView> byField = switch (
                    sort.field() == null ? "" : sort.field()) {
                case "email" -> Comparator.comparing(
                        StudentListItemView::email,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                case "joinedAt" -> Comparator.comparing(
                        StudentListItemView::joinedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                case "gradeLevel" -> Comparator.comparing(
                        StudentListItemView::gradeLevel,
                        Comparator.nullsLast(Enum::compareTo));
                default -> Comparator.comparing(
                        StudentListItemView::displayName,
                        Comparator.nullsLast(String::compareToIgnoreCase));
            };
            if ("DESC".equalsIgnoreCase(sort.direction())) {
                byField = byField.reversed();
            }
            result = result.thenComparing(byField);
        }
        return result;
    }
}