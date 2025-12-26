package com.example.youtube.search.application.impl;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.domain.entity.VideoId;
import com.example.youtube.quota.domain.service.QuotaService;
import com.example.youtube.search.application.SearchUseCase;
import com.example.youtube.search.domain.entity.SearchResult;
import com.example.youtube.search.domain.service.YouTubeSearchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private YouTubeSearchPort youtubeSearchPort;

    @Mock
    private QuotaService quotaService;

    private SearchService searchService;

    private static final String SESSION_ID = "test-session-id";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @BeforeEach
    void setUp() {
        searchService = new SearchService(tokenRepository, youtubeSearchPort, quotaService);
    }

    private Token createValidToken() {
        return Token.create(ACCESS_TOKEN, "refresh-token", 3600L, "Bearer")
                .fold(token -> token, error -> {
                    throw new RuntimeException("Failed to create token");
                });
    }

    @Nested
    class SearchVideos {

        @Test
        void returnsSearchResultsSuccessfully() {
            Token token = createValidToken();
            SearchResult result1 = SearchResult.of(
                    VideoId.fromYouTubeId("vid1"),
                    "Artist - Song Official Video",
                    "Artist Official",
                    "Official music video",
                    null,
                    0.9
            );

            when(tokenRepository.findBySessionId(SESSION_ID)).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST)).thenReturn(Result.successVoid());
            when(youtubeSearchPort.searchVideos(ACCESS_TOKEN, "test query", 10))
                    .thenReturn(Result.success(List.of(result1)));

            var request = new SearchUseCase.SearchRequest("test query", 10);
            Result<List<SearchResult>, Error> result = searchService.searchVideos(SESSION_ID, request);

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    results -> {
                        assertThat(results).hasSize(1);
                        return null;
                    },
                    _ -> null
            );
        }

        @Test
        void filtersOutNonMusicVideos() {
            Token token = createValidToken();
            SearchResult officialVideo = SearchResult.of(
                    VideoId.fromYouTubeId("vid1"),
                    "Artist - Song Official Video",
                    "Artist Official",
                    "Official music video",
                    null,
                    0.9
            );
            SearchResult coverVideo = SearchResult.of(
                    VideoId.fromYouTubeId("vid2"),
                    "Song Cover by Someone",
                    "Cover Artist",
                    "My cover version",
                    null,
                    0.8
            );

            when(tokenRepository.findBySessionId(SESSION_ID)).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST)).thenReturn(Result.successVoid());
            when(youtubeSearchPort.searchVideos(ACCESS_TOKEN, "test query", 10))
                    .thenReturn(Result.success(List.of(officialVideo, coverVideo)));

            var request = new SearchUseCase.SearchRequest("test query", 10);
            Result<List<SearchResult>, Error> result = searchService.searchVideos(SESSION_ID, request);

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    results -> {
                        assertThat(results).hasSize(1);
                        assertThat(results.getFirst().title()).contains("Official");
                        return null;
                    },
                    _ -> null
            );
        }

        @Test
        void failsForBlankQuery() {
            var request = new SearchUseCase.SearchRequest("   ", 10);
            Result<List<SearchResult>, Error> result = searchService.searchVideos(SESSION_ID, request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                        assertThat(((Error.InvalidInputError) error).field()).isEqualTo("query");
                        return null;
                    }
            );
            verify(tokenRepository, never()).findBySessionId(any());
        }

        @Test
        void failsWhenQuotaExceeded() {
            Token token = createValidToken();
            when(tokenRepository.findBySessionId(SESSION_ID)).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST))
                    .thenReturn(Result.failure(Error.quotaExceededError(10000, 10000)));

            var request = new SearchUseCase.SearchRequest("test query", 10);
            Result<List<SearchResult>, Error> result = searchService.searchVideos(SESSION_ID, request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.QuotaExceededError.class);
                        return null;
                    }
            );
        }
    }

    @Nested
    class SearchMusicVideo {

        @Test
        void findsMusicVideoSuccessfully() {
            Token token = createValidToken();
            SearchResult result1 = SearchResult.of(
                    VideoId.fromYouTubeId("vid1"),
                    "Never Gonna Give You Up - Rick Astley",
                    "Rick Astley Official",
                    "Official music video",
                    null,
                    0.95
            );

            when(tokenRepository.findBySessionId(SESSION_ID)).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST)).thenReturn(Result.successVoid());
            when(youtubeSearchPort.searchMusicVideo(ACCESS_TOKEN, "Never Gonna Give You Up", "Rick Astley"))
                    .thenReturn(Result.success(result1));

            var request = new SearchUseCase.MusicSearchRequest("Never Gonna Give You Up", "Rick Astley");
            Result<SearchResult, Error> result = searchService.searchMusicVideo(SESSION_ID, request);

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    searchResult -> {
                        assertThat(searchResult.title()).contains("Rick Astley");
                        return null;
                    },
                    _ -> null
            );
        }

        @Test
        void failsForBlankTrackName() {
            var request = new SearchUseCase.MusicSearchRequest("", "Artist");
            Result<SearchResult, Error> result = searchService.searchMusicVideo(SESSION_ID, request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                        assertThat(((Error.InvalidInputError) error).field()).isEqualTo("trackName");
                        return null;
                    }
            );
        }

        @Test
        void failsForBlankArtistName() {
            var request = new SearchUseCase.MusicSearchRequest("Song", "   ");
            Result<SearchResult, Error> result = searchService.searchMusicVideo(SESSION_ID, request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                        assertThat(((Error.InvalidInputError) error).field()).isEqualTo("artistName");
                        return null;
                    }
            );
        }
    }
}
