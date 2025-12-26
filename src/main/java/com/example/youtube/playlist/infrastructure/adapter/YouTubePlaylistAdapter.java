package com.example.youtube.playlist.infrastructure.adapter;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubePlaylist;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;
import com.example.youtube.playlist.domain.service.YouTubePlaylistPort;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class YouTubePlaylistAdapter implements YouTubePlaylistPort {

    private static final Logger log = LoggerFactory.getLogger(YouTubePlaylistAdapter.class);

    private final NetHttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    public YouTubePlaylistAdapter(NetHttpTransport httpTransport, JsonFactory jsonFactory) {
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Result<List<YouTubePlaylist>, Error> getUserPlaylists(String accessToken) {
        try {
            YouTube youtube = buildAuthenticatedClient(accessToken);

            var response = youtube.playlists()
                    .list(List.of("snippet", "contentDetails"))
                    .setMine(true)
                    .setMaxResults(50L)
                    .execute();

            List<YouTubePlaylist> playlists = new ArrayList<>();
            if (response.getItems() != null) {
                for (Playlist item : response.getItems()) {
                    YouTubePlaylist.create(
                            item.getId(),
                            item.getSnippet().getTitle(),
                            item.getSnippet().getDescription(),
                            item.getSnippet().getChannelId(),
                            item.getSnippet().getChannelTitle(),
                            item.getContentDetails().getItemCount().intValue(),
                            extractThumbnailUrl(item.getSnippet()),
                            parseDateTime(item.getSnippet().getPublishedAt())
                    ).fold(playlists::add,
                            error -> {
                                log.warn("Skipping invalid playlist: {}", error);
                                return null;
                            }
                    );
                }
            }

            return Result.success(playlists);
        } catch (GoogleJsonResponseException e) {
            return handleGoogleError(e, "fetch playlists");
        } catch (IOException e) {
            log.error("Failed to fetch user playlists", e);
            return Result.failure(Error.externalServiceError("YouTube", "Failed to fetch playlists", e));
        }
    }

    @Override
    public Result<PageResult<YouTubeVideo>, Error> getPlaylistVideos(
            String accessToken,
            String playlistId,
            int maxResults,
            String pageToken
    ) {
        try {
            YouTube youtube = buildAuthenticatedClient(accessToken);

            var request = youtube.playlistItems()
                    .list(List.of("snippet", "contentDetails"))
                    .setPlaylistId(playlistId)
                    .setMaxResults((long) Math.min(maxResults, 50));

            if (pageToken != null && !pageToken.isBlank()) {
                request.setPageToken(pageToken);
            }

            var response = request.execute();

            List<YouTubeVideo> videos = new ArrayList<>();
            if (response.getItems() != null) {
                for (PlaylistItem item : response.getItems()) {
                    var snippet = item.getSnippet();
                    YouTubeVideo.create(
                            snippet.getResourceId().getVideoId(),
                            snippet.getTitle(),
                            snippet.getChannelTitle(),
                            snippet.getDescription(),
                            0,
                            extractThumbnailUrl(snippet),
                            parseDateTime(snippet.getPublishedAt())
                    ).fold(videos::add,
                            error -> {
                                log.warn("Skipping invalid video: {}", error);
                                return null;
                            }
                    );
                }
            }

            int total = response.getPageInfo() != null ? response.getPageInfo().getTotalResults() : videos.size();
            return Result.success(PageResult.of(videos, response.getNextPageToken(), total));
        } catch (GoogleJsonResponseException e) {
            return handleGoogleError(e, "fetch playlist videos");
        } catch (IOException e) {
            log.error("Failed to fetch playlist videos", e);
            return Result.failure(Error.externalServiceError("YouTube", "Failed to fetch playlist videos", e));
        }
    }

    @Override
    public Result<YouTubePlaylist, Error> createPlaylist(String accessToken, String title, String description) {
        try {
            YouTube youtube = buildAuthenticatedClient(accessToken);

            Playlist playlist = new Playlist();
            PlaylistSnippet snippet = new PlaylistSnippet();
            snippet.setTitle(title);
            snippet.setDescription(description != null ? description : "");
            playlist.setSnippet(snippet);

            PlaylistStatus status = new PlaylistStatus();
            status.setPrivacyStatus("private");
            playlist.setStatus(status);

            Playlist response = youtube.playlists()
                    .insert(List.of("snippet", "status", "contentDetails"), playlist)
                    .execute();

            return YouTubePlaylist.create(
                    response.getId(),
                    response.getSnippet().getTitle(),
                    response.getSnippet().getDescription(),
                    response.getSnippet().getChannelId(),
                    response.getSnippet().getChannelTitle(),
                    0,
                    extractThumbnailUrl(response.getSnippet()),
                    parseDateTime(response.getSnippet().getPublishedAt())
            );
        } catch (GoogleJsonResponseException e) {
            return handleGoogleError(e, "create playlist");
        } catch (IOException e) {
            log.error("Failed to create playlist", e);
            return Result.failure(Error.externalServiceError("YouTube", "Failed to create playlist", e));
        }
    }

    @Override
    public Result<Void, Error> addVideosToPlaylist(String accessToken, String playlistId, List<String> videoIds) {
        try {
            YouTube youtube = buildAuthenticatedClient(accessToken);

            for (String videoId : videoIds) {
                PlaylistItem playlistItem = new PlaylistItem();
                PlaylistItemSnippet snippet = new PlaylistItemSnippet();
                snippet.setPlaylistId(playlistId);

                ResourceId resourceId = new ResourceId();
                resourceId.setKind("youtube#video");
                resourceId.setVideoId(videoId);
                snippet.setResourceId(resourceId);

                playlistItem.setSnippet(snippet);

                youtube.playlistItems()
                        .insert(List.of("snippet"), playlistItem)
                        .execute();
            }

            return Result.successVoid();
        } catch (GoogleJsonResponseException e) {
            return handleGoogleError(e, "add videos to playlist");
        } catch (IOException e) {
            log.error("Failed to add videos to playlist", e);
            return Result.failure(Error.externalServiceError("YouTube", "Failed to add videos to playlist", e));
        }
    }

    private YouTube buildAuthenticatedClient(String accessToken) {
        HttpRequestInitializer initializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            request.setConnectTimeout(30_000);
            request.setReadTimeout(30_000);
        };

        return new YouTube.Builder(httpTransport, jsonFactory, initializer)
                .setApplicationName("youtube-service")
                .build();
    }

    private <T> Result<T, Error> handleGoogleError(GoogleJsonResponseException e, String operation) {
        int statusCode = e.getStatusCode();
        String message = e.getDetails() != null ? e.getDetails().getMessage() : e.getMessage();

        log.error("YouTube API error during {}: {} - {}", operation, statusCode, message);

        if (statusCode == 401 || statusCode == 403) {
            return Result.failure(Error.authenticationError(
                    "YouTube authentication failed",
                    message
            ));
        }

        if (statusCode == 404) {
            return Result.failure(Error.resourceNotFoundError("YouTube resource", operation));
        }

        return Result.failure(Error.externalServiceError("YouTube", message, e));
    }

    private String extractThumbnailUrl(PlaylistSnippet snippet) {
        if (snippet.getThumbnails() == null) {
            return null;
        }
        if (snippet.getThumbnails().getMedium() != null) {
            return snippet.getThumbnails().getMedium().getUrl();
        }
        if (snippet.getThumbnails().getDefault() != null) {
            return snippet.getThumbnails().getDefault().getUrl();
        }
        return null;
    }

    private String extractThumbnailUrl(PlaylistItemSnippet snippet) {
        if (snippet.getThumbnails() == null) {
            return null;
        }
        if (snippet.getThumbnails().getMedium() != null) {
            return snippet.getThumbnails().getMedium().getUrl();
        }
        if (snippet.getThumbnails().getDefault() != null) {
            return snippet.getThumbnails().getDefault().getUrl();
        }
        return null;
    }

    private Instant parseDateTime(com.google.api.client.util.DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Instant.ofEpochMilli(dateTime.getValue());
    }
}
