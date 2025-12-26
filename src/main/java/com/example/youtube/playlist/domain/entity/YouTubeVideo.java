package com.example.youtube.playlist.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

import java.time.Instant;
import java.util.Objects;

public final class YouTubeVideo {

    private final VideoId id;
    private final String title;
    private final String channelTitle;
    private final String description;
    private final int durationSeconds;
    private final String thumbnailUrl;
    private final Instant publishedAt;

    private YouTubeVideo(
            VideoId id,
            String title,
            String channelTitle,
            String description,
            int durationSeconds,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        this.id = id;
        this.title = title;
        this.channelTitle = channelTitle;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
    }

    public static Result<YouTubeVideo, Error> create(
            String youtubeId,
            String title,
            String channelTitle,
            String description,
            int durationSeconds,
            String thumbnailUrl,
            Instant publishedAt
    ) {
        if (youtubeId == null || youtubeId.isBlank()) {
            return Result.failure(Error.invalidInputError("id", "Video ID cannot be null or empty"));
        }

        if (title == null || title.isBlank()) {
            return Result.failure(Error.invalidInputError("title", "Video title cannot be null or empty"));
        }

        if (durationSeconds < 0) {
            return Result.failure(Error.invalidInputError("durationSeconds", "Duration cannot be negative"));
        }

        VideoId videoId = VideoId.fromYouTubeId(youtubeId);

        return Result.success(new YouTubeVideo(
                videoId,
                title,
                channelTitle,
                Objects.requireNonNullElse(description, ""),
                durationSeconds,
                thumbnailUrl,
                publishedAt
        ));
    }

    public VideoId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String channelTitle() {
        return channelTitle;
    }

    public String description() {
        return description;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public boolean isMusicVideo() {
        String lowerTitle = title.toLowerCase();
        String lowerDesc = description.toLowerCase();
        return !lowerTitle.contains("cover") &&
               !lowerTitle.contains("live") &&
               !lowerTitle.contains("karaoke") &&
               !lowerTitle.contains("instrumental") &&
               !lowerDesc.contains("cover version");
    }
}
