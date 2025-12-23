package com.example.youtube.auth.domain.service;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

public interface PkceGenerator {

    record PkceChallenge(String codeVerifier, String codeChallenge) {}

    Result<PkceChallenge, Error> generate();
}
