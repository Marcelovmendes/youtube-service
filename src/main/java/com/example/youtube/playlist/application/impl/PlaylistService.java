package com.example.youtube.playlist.application.impl;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.application.PlaylistUseCase;
import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubePlaylist;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;
import com.example.youtube.playlist.domain.service.YouTubePlaylistPort;
import com.example.youtube.quota.domain.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaylistService implements PlaylistUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final TokenRepository tokenRepository;
    private final YouTubePlaylistPort youtubePlaylistPort;
    private final QuotaService quotaService;

    public PlaylistService(
            TokenRepository tokenRepository,
            YouTubePlaylistPort youtubePlaylistPort,
            QuotaService quotaService
    ) {
        this.tokenRepository = tokenRepository;
        this.youtubePlaylistPort = youtubePlaylistPort;
        this.quotaService = quotaService;
    }

    @Override
    public Result<List<YouTubePlaylist>, Error> getUserPlaylists(String sessionId) {
        log.info("Fetching user playlists for session: {}", sessionId);

        return validateAndGetToken(sessionId)
                .flatMap(token -> quotaService.consumeQuota(QuotaService.PLAYLISTS_LIST_COST)
                        .flatMap(_ -> youtubePlaylistPort.getUserPlaylists(token.accessToken())));
    }

    @Override
    public Result<PageResult<YouTubeVideo>, Error> getPlaylistVideos(String sessionId, GetVideosRequest request) {
        log.info("Fetching videos for playlist: {}", request.playlistId());

        return validateAndGetToken(sessionId)
                .flatMap(token -> quotaService.consumeQuota(QuotaService.PLAYLIST_ITEMS_LIST_COST)
                        .flatMap(_ -> youtubePlaylistPort.getPlaylistVideos(
                                token.accessToken(),
                                request.playlistId(),
                                request.maxResults(),
                                request.pageToken()
                        )));
    }

    @Override
    public Result<YouTubePlaylist, Error> createPlaylist(String sessionId, CreatePlaylistRequest request) {
        log.info("Creating playlist: {}", request.title());

        if (request.title() == null || request.title().isBlank()) {
            return Result.failure(Error.invalidInputError("title", "Playlist title is required"));
        }

        return validateAndGetToken(sessionId)
                .flatMap(token -> quotaService.consumeQuota(QuotaService.PLAYLISTS_INSERT_COST)
                        .flatMap(_ -> youtubePlaylistPort.createPlaylist(
                                token.accessToken(),
                                request.title(),
                                request.description()
                        )));
    }

    @Override
    public Result<Void, Error> addVideosToPlaylist(String sessionId, AddVideosRequest request) {
        log.info("Adding {} videos to playlist: {}", request.videoIds().size(), request.playlistId());

        if (request.videoIds().isEmpty()) {
            return Result.failure(Error.invalidInputError("videoIds", "At least one video ID is required"));
        }

        int totalCost = request.videoIds().size() * QuotaService.PLAYLIST_ITEMS_INSERT_COST;

        return validateAndGetToken(sessionId)
                .flatMap(token -> quotaService.consumeQuota(totalCost)
                        .flatMap(_ -> youtubePlaylistPort.addVideosToPlaylist(
                                token.accessToken(),
                                request.playlistId(),
                                request.videoIds()
                        )));
    }

    private Result<Token, Error> validateAndGetToken(String sessionId) {
        return tokenRepository.findBySessionId(sessionId)
                .flatMap(token -> {
                    if (!token.isValid()) {
                        return Result.failure(Error.authenticationError(
                                "Token is invalid or expired",
                                "Please re-authenticate"
                        ));
                    }
                    return Result.success(token);
                });
    }
}
