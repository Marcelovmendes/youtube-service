package com.example.youtube.search.domain.service;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.search.domain.entity.SearchResult;

import java.util.List;

public interface YouTubeSearchPort {

    Result<List<SearchResult>, Error> searchVideos(
            String accessToken,
            String query,
            int maxResults
    );

    Result<SearchResult, Error> searchMusicVideo(
            String accessToken,
            String trackName,
            String artistName
    );
}
