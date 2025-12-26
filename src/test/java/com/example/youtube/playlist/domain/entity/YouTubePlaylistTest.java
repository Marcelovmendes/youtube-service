package com.example.youtube.playlist.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubePlaylistTest {

    @Test
    void createsPlaylistSuccessfully() {
        Result<YouTubePlaylist, Error> result = YouTubePlaylist.create(
                "PLtest123",
                "My Playlist",
                "A great playlist",
                "UCchannel123",
                "My Channel",
                10,
                "https://i.ytimg.com/vi/test/mqdefault.jpg",
                Instant.parse("2024-01-15T10:30:00Z")
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                playlist -> {
                    assertThat(playlist.id().youtubeId()).isEqualTo("PLtest123");
                    assertThat(playlist.title()).isEqualTo("My Playlist");
                    assertThat(playlist.description()).isEqualTo("A great playlist");
                    assertThat(playlist.channelId()).isEqualTo("UCchannel123");
                    assertThat(playlist.channelTitle()).isEqualTo("My Channel");
                    assertThat(playlist.itemCount()).isEqualTo(10);
                    return null;
                },
                error -> {
                    throw new AssertionError("Expected success but got: " + error);
                }
        );
    }

    @Test
    void failsForNullId() {
        Result<YouTubePlaylist, Error> result = YouTubePlaylist.create(
                null,
                "My Playlist",
                "Description",
                "UCchannel123",
                "My Channel",
                5,
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
        Result<YouTubePlaylist, Error> result = YouTubePlaylist.create(
                "PLtest123",
                "   ",
                "Description",
                "UCchannel123",
                "My Channel",
                5,
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
    void failsForNegativeItemCount() {
        Result<YouTubePlaylist, Error> result = YouTubePlaylist.create(
                "PLtest123",
                "My Playlist",
                "Description",
                "UCchannel123",
                "My Channel",
                -1,
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
                    assertThat(((Error.InvalidInputError) error).field()).isEqualTo("itemCount");
                    return null;
                }
        );
    }

    @Test
    void handlesNullDescription() {
        Result<YouTubePlaylist, Error> result = YouTubePlaylist.create(
                "PLtest123",
                "My Playlist",
                null,
                "UCchannel123",
                "My Channel",
                0,
                null,
                null
        );

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                playlist -> {
                    assertThat(playlist.description()).isEmpty();
                    return null;
                },
                _ -> {
                    throw new AssertionError("Expected success");
                }
        );
    }
}
