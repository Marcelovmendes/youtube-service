package com.example.youtube.search.api;

import com.example.youtube.common.result.ResultMapper;
import com.example.youtube.search.api.dto.SearchResultResponse;
import com.example.youtube.search.application.SearchUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchUseCase searchUseCase;

    public SearchController(SearchUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;
    }

    @GetMapping
    public ResponseEntity<?> searchVideos(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        var request = new SearchUseCase.SearchRequest(q, maxResults);
        var result = searchUseCase.searchVideos(request);
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
        var result = searchUseCase.searchMusicVideo(request);
        return ResultMapper.toResponse(result, SearchResultResponse::fromDomain);
    }
}
