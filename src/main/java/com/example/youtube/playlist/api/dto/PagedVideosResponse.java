package com.example.youtube.playlist.api.dto;

import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;

import java.util.List;

public record PagedVideosResponse(
        List<VideoResponse> items,
        String nextPageToken,
        int totalResults
) {
    public static PagedVideosResponse fromDomain(PageResult<YouTubeVideo> pageResult) {
        List<VideoResponse> videos = pageResult.items().stream()
                .map(VideoResponse::fromDomain)
                .toList();

        return new PagedVideosResponse(
                videos,
                pageResult.nextPageToken(),
                pageResult.totalResults()
        );
    }
}
