package com.example.youtube.playlist.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

import java.time.Instant;
import java.util.Objects;

public final class YouTubePlaylist {

    private final PlaylistId id;
    private final String title;
    private final String description;
    private final String channelId;
    private final String channelTitle;
    private final int itemCount;
    private final String thumbnailUrl;
    private final Instant publishedAt;

    private YouTubePlaylist(
            PlaylistId id,
            String title,
            String description,
            String channelId,
            String channelTitle,
            int itemCount,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.channelId = channelId;
        this.channelTitle = channelTitle;
        this.itemCount = itemCount;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
    }

    public static Result<YouTubePlaylist, Error> create(
            String youtubeId,
            String title,
            String description,
            String channelId,
            String channelTitle,
            int itemCount,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        if (youtubeId == null || youtubeId.isBlank()) {
            return Result.failure(Error.invalidInputError("id", "Playlist ID cannot be null or empty"));
        }

        if (title == null || title.isBlank()) {
            return Result.failure(Error.invalidInputError("title", "Playlist title cannot be null or empty"));
        }

        if (itemCount < 0) {
            return Result.failure(Error.invalidInputError("itemCount", "Item count cannot be negative"));
        }

        PlaylistId playlistId = PlaylistId.fromYouTubeId(youtubeId);

        return Result.success(new YouTubePlaylist(
                playlistId,
                title,
                Objects.requireNonNullElse(description, ""),
                channelId,
                channelTitle,
                itemCount,
                thumbnailUrl,
                publishedAt
        ));
    }

    public PlaylistId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String channelId() {
        return channelId;
    }

    public String channelTitle() {
        return channelTitle;
    }

    public int itemCount() {
        return itemCount;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public Instant publishedAt() {
        return publishedAt;
    }
}
