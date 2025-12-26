package com.example.youtube.search.api;

import com.example.youtube.common.result.ResultMapper;
import com.example.youtube.search.api.dto.SearchResultResponse;
import com.example.youtube.search.application.SearchUseCase;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube/v1/search")
public class SearchController {

    private final SearchUseCase searchUseCase;
    private final HttpSession httpSession;

    public SearchController(SearchUseCase searchUseCase, HttpSession httpSession) {
        this.searchUseCase = searchUseCase;
        this.httpSession = httpSession;
    }

    @GetMapping
    public ResponseEntity<?> searchVideos(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        var request = new SearchUseCase.SearchRequest(q, maxResults);
        var result = searchUseCase.searchVideos(httpSession.getId(), request);
        return ResultMapper.toResponse(result, results ->
                results.stream().map(SearchResultResponse::fromDomain).toList()
        );
    }

    @GetMapping("/music")
    public ResponseEntity<?> searchMusicVideo(
            @RequestParam String track,
            @RequestParam String artist
    ) {
        var request = new SearchUseCase.MusicSearchRequest(track, artist);
        var result = searchUseCase.searchMusicVideo(httpSession.getId(), request);
        return ResultMapper.toResponse(result, SearchResultResponse::fromDomain);
    }
}
