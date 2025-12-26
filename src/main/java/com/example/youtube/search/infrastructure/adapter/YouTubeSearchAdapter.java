package com.example.youtube.search.infrastructure.adapter;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.domain.entity.VideoId;
import com.example.youtube.search.domain.entity.SearchResult;
import com.example.youtube.search.domain.service.YouTubeSearchPort;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class YouTubeSearchAdapter implements YouTubeSearchPort {

    private static final Logger log = LoggerFactory.getLogger(YouTubeSearchAdapter.class);
    private static final String MUSIC_CATEGORY_ID = "10";

    private final NetHttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    public YouTubeSearchAdapter(NetHttpTransport httpTransport, JsonFactory jsonFactory) {
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Result<List<SearchResult>, Error> searchVideos(String accessToken, String query, int maxResults) {
        try {
            YouTube youtube = buildAuthenticatedClient(accessToken);

            SearchListResponse response = youtube.search()
                    .list(List.of("snippet"))
                    .setQ(query)
                    .setType(List.of("video"))
                    .setVideoCategoryId(MUSIC_CATEGORY_ID)
                    .setMaxResults((long) Math.min(maxResults, 50))
                    .setOrder("relevance")
                    .execute();

            List<SearchResult> results = new ArrayList<>();
            if (response.getItems() != null) {
                int position = 0;
                for (var item : response.getItems()) {
                    String videoId = item.getId().getVideoId();
                    if (videoId == null) {
                        continue;
                    }

                    double relevanceScore = calculateRelevanceScore(position, response.getItems().size());

                    results.add(SearchResult.of(
                            VideoId.fromYouTubeId(videoId),
                            item.getSnippet().getTitle(),
                            item.getSnippet().getChannelTitle(),
                            item.getSnippet().getDescription(),
                            extractThumbnailUrl(item.getSnippet()),
                            relevanceScore
                    ));
                    position++;
                }
            }

            return Result.success(results);
        } catch (GoogleJsonResponseException e) {
            return handleGoogleError(e, "search videos");
        } catch (IOException e) {
            log.error("Failed to search videos", e);
            return Result.failure(Error.externalServiceError("YouTube", "Failed to search videos", e));
        }
    }

    @Override
    public Result<SearchResult, Error> searchMusicVideo(String accessToken, String trackName, String artistName) {
        String query = buildMusicSearchQuery(trackName, artistName);

        return searchVideos(accessToken, query, 5)
                .flatMap(results -> {
                    if (results.isEmpty()) {
                        return Result.failure(Error.resourceNotFoundError(
                                "Music video",
                                trackName + " by " + artistName
                        ));
                    }

                    return results.stream()
                            .filter(SearchResult::isLikelyMusicVideo)
                            .findFirst()
                            .map(Result::<SearchResult, Error>success)
                            .orElseGet(() -> Result.success(results.getFirst()));
                });
    }

    private String buildMusicSearchQuery(String trackName, String artistName) {
        return String.format("%s %s official", trackName, artistName);
    }

    private double calculateRelevanceScore(int position, int totalResults) {
        if (totalResults <= 1) {
            return 1.0;
        }
        return 1.0 - ((double) position / totalResults);
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

    private String extractThumbnailUrl(com.google.api.services.youtube.model.SearchResultSnippet snippet) {
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
}
