package com.example.youtube.auth.application;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface AuthUseCase {

    record AuthInitiationResponse(String authorizationUrl) {}

    record AuthCallbackRequest(String code, String state) {}

    Result<AuthInitiationResponse, Error> initiateAuthentication();

    Result<Token, Error> exchangeCodeForToken(AuthCallbackRequest request);
}
