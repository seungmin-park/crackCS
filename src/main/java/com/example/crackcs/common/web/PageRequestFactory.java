package com.example.crackcs.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PageRequestFactory {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private PageRequestFactory() {
    }

    public static Pageable create(
            Integer page,
            Integer size,
            List<String> sort,
            Set<String> sortableProperties
    ) {
        return PageRequest.of(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                createSort(sort, sortableProperties)
        );
    }

    private static Sort createSort(List<String> sort, Set<String> sortableProperties) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Order.asc("id"));
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (int index = 0; index < sort.size(); index++) {
            String property = sort.get(index);
            Sort.Direction direction = Sort.Direction.ASC;

            if (index + 1 < sort.size() && isDirection(sort.get(index + 1))) {
                direction = Sort.Direction.fromString(sort.get(++index));
            }
            if (!sortableProperties.contains(property)) {
                throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + property);
            }
            orders.add(new Sort.Order(direction, property));
        }
        return Sort.by(orders);
    }

    private static boolean isDirection(String value) {
        return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
    }
}
