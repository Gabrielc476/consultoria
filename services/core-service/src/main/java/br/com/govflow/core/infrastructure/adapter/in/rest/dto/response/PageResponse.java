package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast
) {
    public static <T> PageResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        boolean isFirst = pageNumber == 0;
        boolean isLast = totalPages == 0 || pageNumber >= totalPages - 1;

        return new PageResponse<>(
                content,
                pageNumber,
                pageSize,
                totalElements,
                totalPages,
                isFirst,
                isLast
        );
    }
}
