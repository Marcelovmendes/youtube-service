package com.example.youtube.auth.domain.repository;

import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

import java.time.Duration;

public interface AuthStateRepository {

    Result<Void, Error> save(AuthState state, Duration timeout);

    Result<AuthState, Error> findByStateValue(String stateValue);

    Result<Void, Error> markAsProcessed(String stateValue, Token token);
}
