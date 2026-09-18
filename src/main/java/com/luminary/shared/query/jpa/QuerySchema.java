package com.luminary.shared.query.jpa;

import com.luminary.shared.error.ApiException;
import com.luminary.shared.query.FilterOperator;
import com.luminary.shared.query.SortCriterion;
import com.luminary.shared.query.SortDirection;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-resource allow-list for filtering and sorting. Public request fields are
 * never interpreted as entity paths directly.
 */
public final class QuerySchema<T> {

    private final Map<String, QueryField> fields;
    private final List<Sort.Order> defaultOrders;
    private final String stableSortPath;

    private QuerySchema(Map<String, QueryField> fields,
                        List<Sort.Order> defaultOrders,
                        String stableSortPath) {
        this.fields = Map.copyOf(fields);
        this.defaultOrders = List.copyOf(defaultOrders);
        this.stableSortPath = stableSortPath;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    QueryField filterField(String publicName) {
        QueryField field = fields.get(publicName);
        if (field == null) {
            throw ApiException.badRequest("unsupported-filter-field",
                    "Filtering by field '" + publicName + "' is not supported.");
        }
        return field;
    }

    Sort toSort(List<SortCriterion> criteria) {
        List<Sort.Order> orders = criteria.isEmpty()
                ? new java.util.ArrayList<>(defaultOrders)
                : criteria.stream().map(this::toOrder)
                .collect(java.util.stream.Collectors.toCollection(
                        java.util.ArrayList::new));

        if (stableSortPath != null
                && orders.stream().noneMatch(
                order -> order.getProperty().equals(stableSortPath))) {
            orders.add(Sort.Order.asc(stableSortPath));
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private Sort.Order toOrder(SortCriterion criterion) {
        QueryField field = fields.get(criterion.field());
        if (field == null || !field.sortable()) {
            throw ApiException.badRequest("unsupported-sort-field",
                    "Sorting by field '" + criterion.field()
                            + "' is not supported.");
        }
        return criterion.direction() == SortDirection.DESC
                ? Sort.Order.desc(field.entityPath())
                : Sort.Order.asc(field.entityPath());
    }

    public static final class Builder<T> {

        private final Map<String, QueryField> fields = new LinkedHashMap<>();
        private final List<Sort.Order> defaultOrders = new java.util.ArrayList<>();
        private String stableSortPath;

        public Builder<T> field(String publicName, String entityPath,
                                Class<?> valueType, boolean sortable,
                                FilterOperator... operators) {
            QueryField previous = fields.put(publicName, new QueryField(
                    publicName, entityPath, valueType, sortable,
                    SetSupport.of(operators)));
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate query field: " + publicName);
            }
            return this;
        }

        public Builder<T> defaultSort(String publicName,
                                      SortDirection direction) {
            QueryField field = fields.get(publicName);
            if (field == null || !field.sortable()) {
                throw new IllegalArgumentException(
                        "Default sort field must be registered and sortable: "
                                + publicName);
            }
            defaultOrders.add(direction == SortDirection.DESC
                    ? Sort.Order.desc(field.entityPath())
                    : Sort.Order.asc(field.entityPath()));
            return this;
        }

        /**
         * Adds an internal unique entity path as the final deterministic sort.
         * It does not expose that path to client filtering or sorting.
         */
        public Builder<T> stableSort(String entityPath) {
            if (entityPath == null || entityPath.isBlank()) {
                throw new IllegalArgumentException(
                        "Stable sort path must not be blank");
            }
            this.stableSortPath = entityPath;
            return this;
        }

        public QuerySchema<T> build() {
            return new QuerySchema<>(fields, defaultOrders, stableSortPath);
        }
    }

    private static final class SetSupport {
        private SetSupport() {
        }

        static java.util.Set<FilterOperator> of(FilterOperator[] operators) {
            return operators.length == 0
                    ? java.util.Set.of()
                    : java.util.EnumSet.copyOf(Arrays.asList(operators));
        }
    }
}
