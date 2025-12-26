package com.example.youtube.search.domain.entity;

import com.example.youtube.playlist.domain.entity.VideoId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchResultTest {

    @Test
    void createsSearchResultSuccessfully() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Test Video",
                "Test Channel",
                "Description",
                "https://example.com/thumb.jpg",
                0.85
        );

        assertThat(result.videoId()).isEqualTo(videoId);
        assertThat(result.title()).isEqualTo("Test Video");
        assertThat(result.channelTitle()).isEqualTo("Test Channel");
        assertThat(result.relevanceScore()).isEqualTo(0.85);
    }

    @Test
    void throwsExceptionForNullVideoId() {
        assertThatThrownBy(() -> SearchResult.of(
                null,
                "Test Video",
                "Test Channel",
                "Description",
                null,
                0.5
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsExceptionForNullTitle() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        assertThatThrownBy(() -> SearchResult.of(
                videoId,
                null,
                "Test Channel",
                "Description",
                null,
                0.5
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsExceptionForInvalidRelevanceScoreBelowZero() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        assertThatThrownBy(() -> SearchResult.of(
                videoId,
                "Test",
                "Channel",
                "Desc",
                null,
                -0.1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
    }

    @Test
    void throwsExceptionForInvalidRelevanceScoreAboveOne() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        assertThatThrownBy(() -> SearchResult.of(
                videoId,
                "Test",
                "Channel",
                "Desc",
                null,
                1.1
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
    }

    @Test
    void detectsLikelyMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Artist - Song Name (Official Video)",
                "Artist Official",
                "The official music video",
                null,
                0.9
        );

        assertThat(result.isLikelyMusicVideo()).isTrue();
    }

    @Test
    void detectsCoverVideoAsNotMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Song Name Cover by Someone",
                "Cover Artist",
                "My cover version",
                null,
                0.8
        );

        assertThat(result.isLikelyMusicVideo()).isFalse();
    }

    @Test
    void detectsLivePerformanceAsNotMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Artist - Song Live at Stadium",
                "Artist",
                "Live performance",
                null,
                0.7
        );

        assertThat(result.isLikelyMusicVideo()).isFalse();
    }

    @Test
    void detectsKaraokeAsNotMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Song Name Karaoke Version",
                "Karaoke Channel",
                "Sing along",
                null,
                0.6
        );

        assertThat(result.isLikelyMusicVideo()).isFalse();
    }

    @Test
    void detectsTutorialAsNotMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "How to play Song Name - Guitar Tutorial",
                "Guitar Channel",
                "Learn to play",
                null,
                0.5
        );

        assertThat(result.isLikelyMusicVideo()).isFalse();
    }

    @Test
    void detectsReactionAsNotMusicVideo() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "My Reaction to Artist - Song Name",
                "Reaction Channel",
                "First time hearing",
                null,
                0.4
        );

        assertThat(result.isLikelyMusicVideo()).isFalse();
    }

    @Test
    void handlesNullChannelAndDescription() {
        VideoId videoId = VideoId.fromYouTubeId("abc123");
        SearchResult result = SearchResult.of(
                videoId,
                "Test Video",
                null,
                null,
                null,
                0.5
        );

        assertThat(result.channelTitle()).isEmpty();
        assertThat(result.description()).isEmpty();
    }
}
