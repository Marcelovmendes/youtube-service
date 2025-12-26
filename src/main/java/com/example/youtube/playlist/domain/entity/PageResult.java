package com.example.youtube.playlist.domain.entity;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> items,
        String nextPageToken,
        int totalResults
) {
    public PageResult {
        Objects.requireNonNull(items, "Items list cannot be null");
    }

    public static <T> PageResult<T> of(List<T> items, String nextPageToken, int totalResults) {
        return new PageResult<>(items, nextPageToken, totalResults);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), null, 0);
    }

    public boolean hasNextPage() {
        return nextPageToken != null && !nextPageToken.isBlank();
    }
}
