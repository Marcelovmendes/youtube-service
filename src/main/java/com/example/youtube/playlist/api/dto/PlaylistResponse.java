package com.example.youtube.playlist.api.dto;

import com.example.youtube.playlist.domain.entity.YouTubePlaylist;

import java.time.Instant;

public record PlaylistResponse(
        String id,
        String title,
        String description,
        String channelId,
        String channelTitle,
        int itemCount,
        String thumbnailUrl,
        Instant publishedAt
) {
    public static PlaylistResponse fromDomain(YouTubePlaylist playlist) {
        return new PlaylistResponse(
                playlist.id().youtubeId(),
                playlist.title(),
                playlist.description(),
                playlist.channelId(),
                playlist.channelTitle(),
                playlist.itemCount(),
                playlist.thumbnailUrl(),
                playlist.publishedAt()
        );
    }
}
