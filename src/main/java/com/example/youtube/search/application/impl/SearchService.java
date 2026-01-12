package com.example.youtube.search.application.impl;

import com.example.youtube.auth.application.TokenQuery;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.quota.domain.service.QuotaService;
import com.example.youtube.search.application.SearchUseCase;
import com.example.youtube.search.domain.entity.SearchResult;
import com.example.youtube.search.domain.service.YouTubeSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService implements SearchUseCase {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final TokenQuery tokenQuery;
    private final YouTubeSearchPort youtubeSearchPort;
    private final QuotaService quotaService;

    public SearchService(
            TokenQuery tokenQuery,
            YouTubeSearchPort youtubeSearchPort,
            QuotaService quotaService
    ) {
        this.tokenQuery = tokenQuery;
        this.youtubeSearchPort = youtubeSearchPort;
        this.quotaService = quotaService;
    }

    @Override
    public Result<List<SearchResult>, Error> searchVideos(SearchRequest request) {
        log.info("Searching videos with query: {}", request.query());

        if (request.query() == null || request.query().isBlank()) {
            return Result.failure(Error.invalidInputError("query", "Search query is required"));
        }

        return tokenQuery.getCurrentUserToken()
                .flatMap(token -> quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST)
                        .flatMap(_ -> youtubeSearchPort.searchVideos(
                                token.accessToken(),
                                request.query(),
                                request.maxResults()
                        )))
                .map(results -> results.stream()
                        .filter(SearchResult::isLikelyMusicVideo)
                        .toList());
    }

    @Override
    public Result<SearchResult, Error> searchMusicVideo(MusicSearchRequest request) {
        log.info("Searching music video: {} by {}", request.trackName(), request.artistName());

        if (request.trackName() == null || request.trackName().isBlank()) {
            return Result.failure(Error.invalidInputError("trackName", "Track name is required"));
        }

        if (request.artistName() == null || request.artistName().isBlank()) {
            return Result.failure(Error.invalidInputError("artistName", "Artist name is required"));
        }

        return tokenQuery.getCurrentUserToken()
                .flatMap(token -> quotaService.consumeQuota(QuotaService.SEARCH_LIST_COST)
                        .flatMap(_ -> youtubeSearchPort.searchMusicVideo(
                                token.accessToken(),
                                request.trackName(),
                                request.artistName()
                        )));
    }
}
