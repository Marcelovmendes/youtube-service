package com.example.youtube.search.application;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.search.domain.entity.SearchResult;

import java.util.List;

public interface SearchUseCase {

    record SearchRequest(String query, int maxResults) {
        public SearchRequest {
            if (maxResults <= 0) {
                maxResults = 10;
            }
            if (maxResults > 50) {
                maxResults = 50;
            }
        }
    }

    record MusicSearchRequest(String trackName, String artistName) {}

    Result<List<SearchResult>, Error> searchVideos(SearchRequest request);

    Result<SearchResult, Error> searchMusicVideo(MusicSearchRequest request);
}
