package com.example.youtube.auth.infrastructure.adapter;

import com.example.youtube.auth.domain.service.PkceGenerator;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DefaultPkceGenerator implements PkceGenerator {

    @Override
    public Result<PkceChallenge, Error> generate() {
        try {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            return Result.success(new PkceChallenge(codeVerifier, codeChallenge));
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("PKCE", "Failed to generate PKCE challenge", e));
        }
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
