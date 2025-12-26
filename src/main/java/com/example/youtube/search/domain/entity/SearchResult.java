package com.example.youtube.search.domain.entity;

import com.example.youtube.playlist.domain.entity.VideoId;

import java.util.Objects;

public record SearchResult(
        VideoId videoId,
        String title,
        String channelTitle,
        String description,
        String thumbnailUrl,
        double relevanceScore
) {
    public SearchResult {
        Objects.requireNonNull(videoId, "Video ID cannot be null");
        Objects.requireNonNull(title, "Title cannot be null");
        if (relevanceScore < 0 || relevanceScore > 1) {
            throw new IllegalArgumentException("Relevance score must be between 0 and 1");
        }
    }

    public static SearchResult of(
            VideoId videoId,
            String title,
            String channelTitle,
            String description,
            String thumbnailUrl,
            double relevanceScore
    ) {
        return new SearchResult(
                videoId,
                title,
                Objects.requireNonNullElse(channelTitle, ""),
                Objects.requireNonNullElse(description, ""),
                thumbnailUrl,
                relevanceScore
        );
    }

    public boolean isLikelyMusicVideo() {
        String lowerTitle = title.toLowerCase();
        String lowerDesc = description.toLowerCase();
        return !lowerTitle.contains("cover") &&
               !lowerTitle.contains("live") &&
               !lowerTitle.contains("karaoke") &&
               !lowerTitle.contains("instrumental") &&
               !lowerTitle.contains("tutorial") &&
               !lowerTitle.contains("reaction") &&
               !lowerDesc.contains("cover version");
    }
}
