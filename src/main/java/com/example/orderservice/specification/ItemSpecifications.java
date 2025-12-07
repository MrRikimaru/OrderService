package com.example.orderservice.specification;

import com.example.orderservice.entity.Item;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public final class ItemSpecifications {

    private ItemSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<Item> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Item> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Item> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Item> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            if (minPrice == null) {
                return priceLessThanOrEqual(maxPrice).toPredicate(root, query, criteriaBuilder);
            }
            if (maxPrice == null) {
                return priceGreaterThanOrEqual(minPrice).toPredicate(root, query, criteriaBuilder);
            }
            return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
        };
    }
}