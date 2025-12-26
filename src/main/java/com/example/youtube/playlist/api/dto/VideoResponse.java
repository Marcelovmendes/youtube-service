package com.example.youtube.playlist.api.dto;

import com.example.youtube.playlist.domain.entity.YouTubeVideo;

import java.time.Instant;

public record VideoResponse(
        String id,
        String title,
        String channelTitle,
        String description,
        int durationSeconds,
        String thumbnailUrl,
        Instant publishedAt
) {
    public static VideoResponse fromDomain(YouTubeVideo video) {
        return new VideoResponse(
                video.id().youtubeId(),
                video.title(),
                video.channelTitle(),
                video.description(),
                video.durationSeconds(),
                video.thumbnailUrl(),
                video.publishedAt()
        );
    }
}
