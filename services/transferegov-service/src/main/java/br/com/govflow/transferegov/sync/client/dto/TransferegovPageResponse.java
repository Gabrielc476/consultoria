package br.com.govflow.transferegov.sync.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

public record TransferegovPageResponse<T>(
        @JsonProperty("data") List<T> data,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("total_items") Integer totalItems,
        @JsonProperty("page_number") Integer pageNumber,
        @JsonProperty("page_size") Integer pageSize
) {
    public TransferegovPageResponse {
        if (data == null) {
            data = Collections.emptyList();
        }
        if (totalPages == null) {
            totalPages = 1;
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }
    }

    public boolean hasNext() {
        return totalPages != null && pageNumber != null && pageNumber < totalPages;
    }
}
