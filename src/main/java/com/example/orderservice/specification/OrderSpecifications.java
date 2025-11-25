package com.example.orderservice.specification;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.data.jpa.domain.Specification.allOf;

public class OrderSpecifications {

    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
        };
    }

    public static Specification<Order> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isFalse(root.get("deleted"));
    }

    public static Specification<Order> buildSpecification(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<OrderStatus> statuses) {

        return allOf(
                notDeleted(),
                createdAtBetween(startDate, endDate),
                hasStatusIn(statuses)
        );
    }
}