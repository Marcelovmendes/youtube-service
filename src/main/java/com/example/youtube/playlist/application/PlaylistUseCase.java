package com.example.youtube.playlist.application;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.playlist.domain.entity.PageResult;
import com.example.youtube.playlist.domain.entity.YouTubePlaylist;
import com.example.youtube.playlist.domain.entity.YouTubeVideo;

import java.util.List;

public interface PlaylistUseCase {

    record CreatePlaylistRequest(String title, String description) {}

    record AddVideosRequest(String playlistId, List<String> videoIds) {}

    record GetVideosRequest(String playlistId, int maxResults, String pageToken) {}

    Result<List<YouTubePlaylist>, Error> getUserPlaylists(String sessionId);

    Result<PageResult<YouTubeVideo>, Error> getPlaylistVideos(String sessionId, GetVideosRequest request);

    Result<YouTubePlaylist, Error> createPlaylist(String sessionId, CreatePlaylistRequest request);

    Result<Void, Error> addVideosToPlaylist(String sessionId, AddVideosRequest request);
}
