package com.example.youtube.playlist.domain.entity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record VideoId(String value, UUID internalId) {

    public VideoId {
        Objects.requireNonNull(value, "YouTube video ID cannot be null");
        Objects.requireNonNull(internalId, "Internal ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("YouTube video ID cannot be blank");
        }
    }

    public static VideoId fromYouTubeId(String youtubeId) {
        Objects.requireNonNull(youtubeId, "YouTube video ID cannot be null");
        if (youtubeId.isBlank()) {
            throw new IllegalArgumentException("YouTube video ID cannot be blank");
        }
        UUID internalId = UUID.nameUUIDFromBytes(("youtube:" + youtubeId).getBytes(StandardCharsets.UTF_8));
        return new VideoId(youtubeId, internalId);
    }

    public String youtubeId() {
        return value;
    }
}
