package com.example.youtube.playlist.api;

import com.example.youtube.common.result.ResultMapper;
import com.example.youtube.playlist.api.dto.AddVideosRequest;
import com.example.youtube.playlist.api.dto.CreatePlaylistRequest;
import com.example.youtube.playlist.api.dto.PagedVideosResponse;
import com.example.youtube.playlist.api.dto.PlaylistResponse;
import com.example.youtube.playlist.application.PlaylistUseCase;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube/v1/playlists")
public class PlaylistController {

    private final PlaylistUseCase playlistUseCase;
    private final HttpSession httpSession;

    public PlaylistController(PlaylistUseCase playlistUseCase, HttpSession httpSession) {
        this.playlistUseCase = playlistUseCase;
        this.httpSession = httpSession;
    }

    @GetMapping
    public ResponseEntity<?> getUserPlaylists() {
        var result = playlistUseCase.getUserPlaylists(httpSession.getId());
        return ResultMapper.toResponse(result, playlists ->
                playlists.stream().map(PlaylistResponse::fromDomain).toList()
        );
    }

    @GetMapping("/{playlistId}/videos")
    public ResponseEntity<?> getPlaylistVideos(
            @PathVariable String playlistId,
            @RequestParam(defaultValue = "25") int maxResults,
            @RequestParam(required = false) String pageToken
    ) {
        var request = new PlaylistUseCase.GetVideosRequest(playlistId, maxResults, pageToken);
        var result = playlistUseCase.getPlaylistVideos(httpSession.getId(), request);
        return ResultMapper.toResponse(result, PagedVideosResponse::fromDomain);
    }

    @PostMapping
    public ResponseEntity<?> createPlaylist(@RequestBody CreatePlaylistRequest request) {
        var useCaseRequest = new PlaylistUseCase.CreatePlaylistRequest(
                request.title(),
                request.description()
        );
        var result = playlistUseCase.createPlaylist(httpSession.getId(), useCaseRequest);
        return ResultMapper.created(result, PlaylistResponse::fromDomain);
    }

    @PostMapping("/{playlistId}/videos")
    public ResponseEntity<?> addVideosToPlaylist(
            @PathVariable String playlistId,
            @RequestBody AddVideosRequest request
    ) {
        var useCaseRequest = new PlaylistUseCase.AddVideosRequest(playlistId, request.videoIds());
        var result = playlistUseCase.addVideosToPlaylist(httpSession.getId(), useCaseRequest);
        return ResultMapper.toResponse(result, _ -> null);
    }
}
