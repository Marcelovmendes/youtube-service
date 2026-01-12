package com.example.youtube.playlist.application.impl;

import com.example.youtube.auth.application.TokenQuery;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.application.PlaylistUseCase;
import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubePlaylist;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;
import com.example.youtube.playlist.domain.service.YouTubePlaylistPort;
import com.example.youtube.quota.domain.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private TokenQuery tokenQuery;

    @Mock
    private YouTubePlaylistPort youtubePlaylistPort;

    @Mock
    private QuotaService quotaService;

    private PlaylistService playlistService;

    private static final String ACCESS_TOKEN = "valid-access-token";

    @BeforeEach
    void setUp() {
        playlistService = new PlaylistService(tokenQuery, youtubePlaylistPort, quotaService);
    }

    private Token createValidToken() {
        return Token.create(ACCESS_TOKEN, "refresh-token", 3600L, "Bearer")
                .fold(token -> token, error -> {
                    throw new RuntimeException("Failed to create token");
                });
    }

    @Nested
    class GetUserPlaylists {

        @Test
        void returnsPlaylistsSuccessfully() {
            Token token = createValidToken();
            YouTubePlaylist playlist = YouTubePlaylist.create(
                    "PLtest123", "My Playlist", "Description",
                    "UCchannel", "My Channel", 5, null, Instant.now()
            ).fold(p -> p, e -> null);

            when(tokenQuery.getCurrentUserToken()).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.PLAYLISTS_LIST_COST)).thenReturn(Result.successVoid());
            when(youtubePlaylistPort.getUserPlaylists(ACCESS_TOKEN)).thenReturn(Result.success(List.of(playlist)));

            Result<List<YouTubePlaylist>, Error> result = playlistService.getUserPlaylists();

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    playlists -> {
                        assertThat(playlists).hasSize(1);
                        assertThat(playlists.getFirst().title()).isEqualTo("My Playlist");
                        return null;
                    },
                    _ -> null
            );
        }

        @Test
        void failsWhenTokenNotFound() {
            when(tokenQuery.getCurrentUserToken())
                    .thenReturn(Result.failure(Error.authenticationError("No active session", "Please authenticate first")));

            Result<List<YouTubePlaylist>, Error> result = playlistService.getUserPlaylists();

            assertThat(result.isFailure()).isTrue();
            verify(quotaService, never()).consumeQuota(anyInt());
            verify(youtubePlaylistPort, never()).getUserPlaylists(any());
        }

        @Test
        void failsWhenQuotaExceeded() {
            Token token = createValidToken();
            when(tokenQuery.getCurrentUserToken()).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.PLAYLISTS_LIST_COST))
                    .thenReturn(Result.failure(Error.quotaExceededError(10000, 10000)));

            Result<List<YouTubePlaylist>, Error> result = playlistService.getUserPlaylists();

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.QuotaExceededError.class);
                        return null;
                    }
            );
            verify(youtubePlaylistPort, never()).getUserPlaylists(any());
        }
    }

    @Nested
    class CreatePlaylist {

        @Test
        void createsPlaylistSuccessfully() {
            Token token = createValidToken();
            YouTubePlaylist createdPlaylist = YouTubePlaylist.create(
                    "PLnew123", "New Playlist", "My new playlist",
                    "UCchannel", "My Channel", 0, null, Instant.now()
            ).fold(p -> p, e -> null);

            when(tokenQuery.getCurrentUserToken()).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.PLAYLISTS_INSERT_COST)).thenReturn(Result.successVoid());
            when(youtubePlaylistPort.createPlaylist(ACCESS_TOKEN, "New Playlist", "My new playlist"))
                    .thenReturn(Result.success(createdPlaylist));

            var request = new PlaylistUseCase.CreatePlaylistRequest("New Playlist", "My new playlist");
            Result<YouTubePlaylist, Error> result = playlistService.createPlaylist(request);

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    playlist -> {
                        assertThat(playlist.title()).isEqualTo("New Playlist");
                        return null;
                    },
                    _ -> null
            );
        }

        @Test
        void failsForBlankTitle() {
            var request = new PlaylistUseCase.CreatePlaylistRequest("   ", "Description");
            Result<YouTubePlaylist, Error> result = playlistService.createPlaylist(request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                        return null;
                    }
            );
            verify(tokenQuery, never()).getCurrentUserToken();
        }
    }

    @Nested
    class AddVideosToPlaylist {

        @Test
        void addsVideosSuccessfully() {
            Token token = createValidToken();
            List<String> videoIds = List.of("video1", "video2");

            when(tokenQuery.getCurrentUserToken()).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(videoIds.size() * QuotaService.PLAYLIST_ITEMS_INSERT_COST))
                    .thenReturn(Result.successVoid());
            when(youtubePlaylistPort.addVideosToPlaylist(ACCESS_TOKEN, "PLtest123", videoIds))
                    .thenReturn(Result.successVoid());

            var request = new PlaylistUseCase.AddVideosRequest("PLtest123", videoIds);
            Result<Void, Error> result = playlistService.addVideosToPlaylist(request);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        void failsForEmptyVideoIds() {
            var request = new PlaylistUseCase.AddVideosRequest("PLtest123", List.of());
            Result<Void, Error> result = playlistService.addVideosToPlaylist(request);

            assertThat(result.isFailure()).isTrue();
            result.fold(
                    _ -> null,
                    error -> {
                        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
                        assertThat(((Error.InvalidInputError) error).field()).isEqualTo("videoIds");
                        return null;
                    }
            );
        }
    }

    @Nested
    class GetPlaylistVideos {

        @Test
        void returnsVideosSuccessfully() {
            Token token = createValidToken();
            YouTubeVideo video = YouTubeVideo.create(
                    "vid123", "Song Title", "Artist", "Description",
                    200, null, Instant.now()
            ).fold(v -> v, e -> null);

            PageResult<YouTubeVideo> pageResult = PageResult.of(List.of(video), "nextToken", 10);

            when(tokenQuery.getCurrentUserToken()).thenReturn(Result.success(token));
            when(quotaService.consumeQuota(QuotaService.PLAYLIST_ITEMS_LIST_COST)).thenReturn(Result.successVoid());
            when(youtubePlaylistPort.getPlaylistVideos(eq(ACCESS_TOKEN), eq("PLtest123"), eq(25), any()))
                    .thenReturn(Result.success(pageResult));

            var request = new PlaylistUseCase.GetVideosRequest("PLtest123", 25, null);
            Result<PageResult<YouTubeVideo>, Error> result = playlistService.getPlaylistVideos(request);

            assertThat(result.isSuccess()).isTrue();
            result.fold(
                    page -> {
                        assertThat(page.items()).hasSize(1);
                        assertThat(page.hasNextPage()).isTrue();
                        return null;
                    },
                    _ -> null
            );
        }
    }
}
