package com.example.youtube.playlist.domain.service;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubePlaylist;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;

import java.util.List;

public interface YouTubePlaylistPort {

    Result<List<YouTubePlaylist>, Error> getUserPlaylists(String accessToken);

    Result<PageResult<YouTubeVideo>, Error> getPlaylistVideos(
            String accessToken,
            String playlistId,
            int maxResults,
            String pageToken
    );

    Result<YouTubePlaylist, Error> createPlaylist(
            String accessToken,
            String title,
            String description
    );

    Result<Void, Error> addVideosToPlaylist(
            String accessToken,
            String playlistId,
            List<String> videoIds
    );
}
