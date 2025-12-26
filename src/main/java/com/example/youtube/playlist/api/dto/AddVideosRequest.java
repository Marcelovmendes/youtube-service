package com.example.youtube.playlist.api.dto;

import java.util.List;

public record AddVideosRequest(
        List<String> videoIds
) {}
