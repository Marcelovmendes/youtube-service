package com.example.youtube.auth.application;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface TokenQuery {

    Result<Token, Error> getCurrentUserToken();

    boolean isUserAuthenticated();

    Result<Void, Error> storeUserToken(String sessionId, Token token);

    Result<Token, Error> refreshToken(String sessionId);

    Result<Void, Error> removeToken(String sessionId);
}
