package com.example.youtube.auth.domain.service;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface OAuthClient {

    Result<String, Error> buildAuthorizationUrl(String state, String codeChallenge);

    Result<Token, Error> exchangeCodeForToken(String code, String codeVerifier);

    Result<Token, Error> refreshToken(String refreshToken);
}
