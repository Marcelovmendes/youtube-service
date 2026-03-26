package com.example.youtube.auth.api;

import com.example.youtube.auth.api.dto.TokenResponse;
import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.auth.application.TokenQuery;
import com.example.youtube.common.result.ResultMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendBaseUrl;

    private final AuthUseCase authUseCase;
    private final TokenQuery tokenQuery;

    public AuthenticationController(AuthUseCase authUseCase, TokenQuery tokenQuery) {
        this.authUseCase = authUseCase;
        this.tokenQuery = tokenQuery;
    }

    private String buildRedirectHtml(String url) {
        return "<!DOCTYPE html><html><body><script>window.location.replace(\"" + url + "\");</script></body></html>";
    }

    @GetMapping
    public ResponseEntity<?> initiateAuthentication() {
        return ResultMapper.ok(authUseCase.initiateAuthentication());
    }

    @GetMapping("/google/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session
    ) {
        log.info("OAuth callback received - Session ID: {}", session.getId());
        log.info("Callback parameters - Code present: {}, State: {}, Error: {}",
                code != null, state, error);

        if (error != null) {
            log.error("OAuth provider returned error: {}", error);
            String encoded = URLEncoder.encode(error, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(buildRedirectHtml(frontendBaseUrl + "/auth/youtube/callback?status=error&message=" + encoded));
        }

        return authUseCase.exchangeCodeForToken(
                new AuthUseCase.AuthCallbackRequest(code, state)
        ).fold(
                token -> {
                    tokenQuery.storeUserToken(session.getId(), token);
                    log.info("Authentication successful - Token stored in session: {}", session.getId());
                    log.info("Session isNew: {}, maxInactiveInterval: {}", session.isNew(), session.getMaxInactiveInterval());
                    return ResponseEntity.ok()
                            .header("Content-Type", "text/html")
                            .body(buildRedirectHtml(frontendBaseUrl + "/auth/youtube/callback?status=success"));
                },
                authError -> {
                    log.error("Authentication failed: {}", authError.message());
                    String encoded = URLEncoder.encode(authError.message(), StandardCharsets.UTF_8);
                    return ResponseEntity.ok()
                            .header("Content-Type", "text/html")
                            .body(buildRedirectHtml(frontendBaseUrl + "/auth/youtube/callback?status=error&message=" + encoded));
                }
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpSession session) {
        return ResultMapper.toResponse(
                tokenQuery.refreshToken(session.getId()),
                TokenResponse::fromToken
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        tokenQuery.removeToken(session.getId());
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/session")
    public ResponseEntity<?> getSession(HttpSession session) {
        if (!tokenQuery.isUserAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new SessionResponse(session.getId()));
    }

    public record SessionResponse(String sessionId) {}
}
