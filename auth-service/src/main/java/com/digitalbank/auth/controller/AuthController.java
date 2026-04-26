package com.digitalbank.auth.controller;

import com.digitalbank.auth.dto.*;
import com.digitalbank.auth.service.AuthService;
import com.digitalbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Kimlik doğrulama endpoint'leri.
 *
 * REST API tasarım prensipleri:
 * - POST /auth/register   → 201 Created (kayıt başarılı)
 * - POST /auth/login      → 200 OK (token döner)
 * - POST /auth/refresh    → 200 OK (yeni token)
 * - POST /auth/logout     → 200 OK (token iptal)
 * - GET  /auth/me         → 200 OK (mevcut kullanıcı bilgisi)
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Yeni kullanıcı kaydı.
     * @Valid: RegisterRequest'teki Bean Validation kurallarını tetikler.
     * Hatalı girişte GlobalExceptionHandler 400 döndürür.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Kayıt isteği: {}", request.getEmail());
        TokenResponse tokenResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kayıt başarılı", tokenResponse));
    }

    /**
     * Kullanıcı girişi.
     * Başarılı giriş sonrası access + refresh token döner.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Giriş isteği: {}", request.getEmail());
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Giriş başarılı", tokenResponse));
    }

    /**
     * Access token yenileme.
     * Refresh token ile yeni bir access token alınır.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token yenilendi", tokenResponse));
    }

    /**
     * Çıkış yapma.
     * @AuthenticationPrincipal: SecurityContext'teki kullanıcı ID'sini inject eder.
     * JwtAuthenticationFilter'da principal olarak userId set edilmişti.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String userId) {

        authService.logout(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Çıkış başarılı"));
    }

    /**
     * Mevcut kullanıcı bilgisini döner.
     * Token doğrulandıktan sonra SecurityContext'ten principal alınır.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> me(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Kullanıcı bilgisi", userId));
    }
}
