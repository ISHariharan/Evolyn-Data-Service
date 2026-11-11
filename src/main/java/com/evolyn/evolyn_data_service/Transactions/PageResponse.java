package com.evolyn.evolyn_data_service.Transactions;

import java.util.List;

import org.springframework.data.domain.Page;

/** A stable JSON shape for one page of results. */
public record PageResponse<T>(
        List<T> items,
        int page,          // 0-based
        int size,
        long totalItems,
        int totalPages) {

    static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
