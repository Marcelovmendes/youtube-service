package com.example.youtube.auth.domain.repository;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface TokenRepository {

    Result<Void, Error> save(String sessionId, Token token);

    Result<Token, Error> findBySessionId(String sessionId);

    Result<Void, Error> remove(String sessionId);
}
