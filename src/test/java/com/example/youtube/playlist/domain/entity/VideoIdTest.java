package com.example.youtube.playlist.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoIdTest {

    @Test
    void createsVideoIdFromYouTubeId() {
        VideoId videoId = VideoId.fromYouTubeId("dQw4w9WgXcQ");

        assertThat(videoId.value()).isEqualTo("dQw4w9WgXcQ");
        assertThat(videoId.youtubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(videoId.internalId()).isNotNull();
    }

    @Test
    void generatesDeterministicInternalId() {
        VideoId videoId1 = VideoId.fromYouTubeId("dQw4w9WgXcQ");
        VideoId videoId2 = VideoId.fromYouTubeId("dQw4w9WgXcQ");

        assertThat(videoId1.internalId()).isEqualTo(videoId2.internalId());
    }

    @Test
    void generatesDifferentInternalIdsForDifferentYouTubeIds() {
        VideoId videoId1 = VideoId.fromYouTubeId("dQw4w9WgXcQ");
        VideoId videoId2 = VideoId.fromYouTubeId("abc123XYZ");

        assertThat(videoId1.internalId()).isNotEqualTo(videoId2.internalId());
    }

    @Test
    void throwsExceptionForNullYouTubeId() {
        assertThatThrownBy(() -> VideoId.fromYouTubeId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("null");
    }

    @Test
    void throwsExceptionForBlankYouTubeId() {
        assertThatThrownBy(() -> VideoId.fromYouTubeId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }
}
