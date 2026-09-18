package com.luminary.shared.query.jpa;

import com.luminary.shared.error.ApiException;
import com.luminary.shared.query.FilterCriterion;
import com.luminary.shared.query.FilterOperator;
import com.luminary.shared.query.SearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Converts the shared query contract into safe JPA specifications and an
 * offset-based pageable. Only paths registered in a {@link QuerySchema} can
 * reach the Criteria API.
 */
public final class JpaQueryBuilder {

    private static final char LIKE_ESCAPE = '\\';

    private JpaQueryBuilder() {
    }

    public static <T> Specification<T> specification(
            SearchRequest request, QuerySchema<T> schema) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (FilterCriterion criterion : request.filters()) {
                predicates.add(toPredicate(root, criteriaBuilder,
                        criterion, schema));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static <T> Pageable pageable(SearchRequest request,
                                        QuerySchema<T> schema) {
        return new OffsetPageRequest(request.skip(), request.take(),
                schema.toSort(request.sorts()));
    }

    private static <T> Predicate toPredicate(
            Root<T> root,
            CriteriaBuilder builder,
            FilterCriterion criterion,
            QuerySchema<T> schema) {
        QueryField field = schema.filterField(criterion.field());
        FilterOperator operator = criterion.operator();
        if (!field.operators().contains(operator)) {
            throw ApiException.badRequest("unsupported-filter-operator",
                    "Operator '" + operator + "' is not supported for field '"
                            + criterion.field() + "'.");
        }

        Path<?> path = resolvePath(root, field.entityPath());
        return switch (operator) {
            case EQUALS -> builder.equal(path, scalarValue(criterion, field));
            case NOT_EQUALS -> builder.notEqual(path,
                    scalarValue(criterion, field));
            case CONTAINS -> like(builder, path, criterion, field, true);
            case STARTS_WITH -> like(builder, path, criterion, field, false);
            case GREATER_THAN -> compare(builder, path,
                    scalarValue(criterion, field), Comparison.GREATER_THAN);
            case GREATER_THAN_OR_EQUAL -> compare(builder, path,
                    scalarValue(criterion, field),
                    Comparison.GREATER_THAN_OR_EQUAL);
            case LESS_THAN -> compare(builder, path,
                    scalarValue(criterion, field), Comparison.LESS_THAN);
            case LESS_THAN_OR_EQUAL -> compare(builder, path,
                    scalarValue(criterion, field),
                    Comparison.LESS_THAN_OR_EQUAL);
            case IN -> path.in(collectionValues(criterion, field, false));
            case BETWEEN -> between(builder, path,
                    collectionValues(criterion, field, true));
            case IS_NULL -> builder.isNull(path);
            case IS_NOT_NULL -> builder.isNotNull(path);
        };
    }

    private static Predicate like(CriteriaBuilder builder, Path<?> path,
                                  FilterCriterion criterion,
                                  QueryField field,
                                  boolean contains) {
        String value = (String) scalarValue(criterion, field);
        String escaped = escapeLike(value.toLowerCase(Locale.ROOT));
        String pattern = contains ? "%" + escaped + "%" : escaped + "%";
        return builder.like(builder.lower(path.as(String.class)), pattern,
                LIKE_ESCAPE);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate compare(CriteriaBuilder builder, Path<?> path,
                                     Object value, Comparison comparison) {
        Expression expression = path;
        Comparable comparable = (Comparable) value;
        return switch (comparison) {
            case GREATER_THAN -> builder.greaterThan(expression, comparable);
            case GREATER_THAN_OR_EQUAL -> builder.greaterThanOrEqualTo(
                    expression, comparable);
            case LESS_THAN -> builder.lessThan(expression, comparable);
            case LESS_THAN_OR_EQUAL -> builder.lessThanOrEqualTo(
                    expression, comparable);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate between(CriteriaBuilder builder, Path<?> path,
                                     List<Object> values) {
        Expression expression = path;
        return builder.between(expression, (Comparable) values.get(0),
                (Comparable) values.get(1));
    }

    private static Object scalarValue(FilterCriterion criterion,
                                      QueryField field) {
        if (criterion.value() == null) {
            throw invalidValue(criterion,
                    "A scalar value is required for this operator.");
        }
        return convert(criterion.value(), field.valueType(), criterion);
    }

    private static List<Object> collectionValues(FilterCriterion criterion,
                                                 QueryField field,
                                                 boolean exactlyTwo) {
        List<Object> rawValues = criterion.values();
        if (rawValues.isEmpty()) {
            throw invalidValue(criterion,
                    "A non-empty values array is required for this operator.");
        }
        if (exactlyTwo && rawValues.size() != 2) {
            throw invalidValue(criterion,
                    "BETWEEN requires exactly two values.");
        }
        return rawValues.stream()
                .map(value -> convert(value, field.valueType(), criterion))
                .toList();
    }

    private static Object convert(Object raw, Class<?> targetType,
                                  FilterCriterion criterion) {
        if (raw == null) {
            throw invalidValue(criterion, "Filter values cannot be null.");
        }
        if (targetType.isInstance(raw)) {
            return raw;
        }

        String text = String.valueOf(raw).trim();
        try {
            if (targetType == String.class) {
                return String.valueOf(raw);
            }
            if (targetType == UUID.class) {
                return UUID.fromString(text);
            }
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(text);
            }
            if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(text);
            }
            if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(text);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(text);
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                if (!text.equalsIgnoreCase("true")
                        && !text.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Not a boolean");
                }
                return Boolean.valueOf(text);
            }
            if (targetType == OffsetDateTime.class) {
                return OffsetDateTime.parse(text);
            }
            if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(text);
            }
            if (targetType == LocalDate.class) {
                return LocalDate.parse(text);
            }
            if (targetType == Instant.class) {
                return Instant.parse(text);
            }
            if (targetType.isEnum()) {
                return enumValue(targetType, text);
            }
        } catch (RuntimeException exception) {
            throw invalidValue(criterion,
                    "Value '" + text + "' is not a valid "
                            + targetType.getSimpleName() + ".");
        }

        throw new IllegalStateException(
                "Unsupported query field type: " + targetType.getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Enum<?> enumValue(Class<?> targetType, String text) {
        return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(
                Enum.class), text.toUpperCase(Locale.ROOT));
    }

    private static Path<?> resolvePath(Path<?> root, String entityPath) {
        Path<?> result = root;
        for (String segment : entityPath.split("\\.")) {
            result = result.get(segment);
        }
        return result;
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static ApiException invalidValue(FilterCriterion criterion,
                                             String detail) {
        return ApiException.badRequest("invalid-filter-value",
                "Invalid value for filter field '" + criterion.field()
                        + "'. " + detail);
    }

    private enum Comparison {
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL
    }
}
