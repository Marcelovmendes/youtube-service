package com.example.youtube.auth.api;

import com.example.youtube.auth.api.dto.TokenResponse;
import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.common.result.ResultMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthUseCase authUseCase;

    public AuthenticationController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @GetMapping
    public ResponseEntity<?> initiateAuthentication() {
        return ResultMapper.ok(authUseCase.initiateAuthentication());
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResultMapper.ErrorDTO("AUTHORIZATION_DENIED", "User denied authorization", error));
        }

        return ResultMapper.toResponse(
                authUseCase.handleCallback(new AuthUseCase.AuthCallbackRequest(code, state)),
                TokenResponse::fromToken
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpSession session) {
        return ResultMapper.toResponse(
                authUseCase.refreshToken(session.getId()),
                TokenResponse::fromToken
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}
