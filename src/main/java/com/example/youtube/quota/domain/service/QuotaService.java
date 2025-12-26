package com.example.youtube.quota.domain.service;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface QuotaService {

    long DAILY_QUOTA_LIMIT = 10_000;

    int SEARCH_LIST_COST = 100;
    int PLAYLISTS_LIST_COST = 1;
    int PLAYLISTS_INSERT_COST = 50;
    int PLAYLIST_ITEMS_INSERT_COST = 50;
    int PLAYLIST_ITEMS_LIST_COST = 1;

    Result<Void, Error> consumeQuota(int units);

    Result<Long, Error> getCurrentUsage();

    Result<Long, Error> getRemainingQuota();

    Result<Boolean, Error> hasAvailableQuota(int requiredUnits);
}
