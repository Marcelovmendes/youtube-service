package com.example.youtube.playlist.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeVideoTest {

    @Test
    void createsVideoSuccessfully() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "dQw4w9WgXcQ",
                "Never Gonna Give You Up",
                "Rick Astley",
                "The official video",
                213,
                "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg",
                Instant.parse("2009-10-25T06:57:33Z")
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                video -> {
                    assertThat(video.id().youtubeId()).isEqualTo("dQw4w9WgXcQ");
                    assertThat(video.title()).isEqualTo("Never Gonna Give You Up");
                    assertThat(video.channelTitle()).isEqualTo("Rick Astley");
                    assertThat(video.durationSeconds()).isEqualTo(213);
                    return null;
                },
                error -> {
                    throw new AssertionError("Expected success but got: " + error);
                }
        );
    }

    @Test
    void failsForNullVideoId() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                null,
                "Some Title",
                "Some Channel",
                "Description",
                180,
                null,
                Instant.now()
        );

        assertThat(result.isFailure()).isTrue();
        result.fold(
                _ -> {
                    throw new AssertionError("Expected failure");
                },
                error -> {
                    assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                    assertThat(((Error.InvalidInputError) error).field()).isEqualTo("id");
                    return null;
                }
        );
    }

    @Test
    void failsForBlankTitle() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "abc123",
                "",
                "Some Channel",
                "Description",
                180,
                null,
                Instant.now()
        );

        assertThat(result.isFailure()).isTrue();
        result.fold(
                _ -> {
                    throw new AssertionError("Expected failure");
                },
                error -> {
                    assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                    assertThat(((Error.InvalidInputError) error).field()).isEqualTo("title");
                    return null;
                }
        );
    }

    @Test
    void failsForNegativeDuration() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "abc123",
                "Some Title",
                "Some Channel",
                "Description",
                -10,
                null,
                Instant.now()
        );

        assertThat(result.isFailure()).isTrue();
        result.fold(
                _ -> {
                    throw new AssertionError("Expected failure");
                },
                error -> {
                    assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                    assertThat(((Error.InvalidInputError) error).field()).isEqualTo("durationSeconds");
                    return null;
                }
        );
    }

    @Test
    void detectsMusicVideo() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "abc123",
                "Artist - Song Name (Official Video)",
                "Artist Official",
                "Official music video",
                240,
                null,
                Instant.now()
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                video -> {
                    assertThat(video.isMusicVideo()).isTrue();
                    return null;
                },
                _ -> null
        );
    }

    @Test
    void detectsLiveVideo() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "abc123",
                "Artist - Song Name (Live at Wembley)",
                "Artist Official",
                "Live performance",
                300,
                null,
                Instant.now()
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                video -> {
                    assertThat(video.isMusicVideo()).isFalse();
                    return null;
                },
                _ -> null
        );
    }

    @Test
    void detectsCoverVersion() {
        Result<YouTubeVideo, Error> result = YouTubeVideo.create(
                "abc123",
                "Song Name Cover by Someone",
                "Cover Artist",
                "My cover version of the song",
                200,
                null,
                Instant.now()
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                video -> {
                    assertThat(video.isMusicVideo()).isFalse();
                    return null;
                },
                _ -> null
        );
    }
}
