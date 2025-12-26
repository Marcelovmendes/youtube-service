package com.example.youtube.search.api.dto;

import com.example.youtube.search.domain.entity.SearchResult;

public record SearchResultResponse(
        String videoId,
        String title,
        String channelTitle,
        String description,
        String thumbnailUrl,
        double relevanceScore
) {
    public static SearchResultResponse fromDomain(SearchResult result) {
        return new SearchResultResponse(
                result.videoId().youtubeId(),
                result.title(),
                result.channelTitle(),
                result.description(),
                result.thumbnailUrl(),
                result.relevanceScore()
        );
    }
}
