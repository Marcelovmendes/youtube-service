package com.example.youtube.playlist.domain.entity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record PlaylistId(String value, UUID internalId) {

    public PlaylistId {
        Objects.requireNonNull(value, "YouTube playlist ID cannot be null");
        Objects.requireNonNull(internalId, "Internal ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("YouTube playlist ID cannot be blank");
        }
    }

    public static PlaylistId fromYouTubeId(String youtubeId) {
        Objects.requireNonNull(youtubeId, "YouTube playlist ID cannot be null");
        if (youtubeId.isBlank()) {
            throw new IllegalArgumentException("YouTube playlist ID cannot be blank");
        }
        UUID internalId = UUID.nameUUIDFromBytes(("youtube:playlist:" + youtubeId).getBytes(StandardCharsets.UTF_8));
        return new PlaylistId(youtubeId, internalId);
    }

    public String youtubeId() {
        return value;
    }
}
