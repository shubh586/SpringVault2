package com.appvault.server.controller;

import com.appvault.server.model.dto.ApiResponse;
import com.appvault.server.model.dto.TokenRequest;
import com.appvault.server.model.dto.TokenResponse;
import com.appvault.server.service.ServiceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ServiceTokenController {

    private final ServiceTokenService serviceTokenService;

    /**
     * Generate a new service token (admin only).
     * The raw token is returned ONCE — store it securely.
     *
     * POST /api/v1/auth/token
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> generateToken(
            @Valid @RequestBody TokenRequest request) {

        TokenResponse response = serviceTokenService.generateToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Token generated. Store it securely — it won't be shown again.", response));
    }
}
