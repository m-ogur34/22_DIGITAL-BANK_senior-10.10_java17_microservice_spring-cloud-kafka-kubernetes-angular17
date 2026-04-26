package com.digitalbank.account.controller;

import com.digitalbank.account.dto.AccountResponse;
import com.digitalbank.account.dto.CreateAccountRequest;
import com.digitalbank.account.service.AccountService;
import com.digitalbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Hesap yönetimi endpoint'leri.
 *
 * REST tasarımı:
 * POST   /accounts               → Yeni hesap aç
 * GET    /accounts               → Hesaplarımı listele
 * GET    /accounts/{iban}/balance → Bakiye sorgula (Redis cache'li)
 * PUT    /accounts/{iban}/freeze  → Hesabı dondur (EMPLOYEE/ADMIN)
 * PUT    /accounts/{iban}/activate → Hesabı aktifleştir (EMPLOYEE/ADMIN)
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal String userId) {

        log.info("Hesap açma isteği: Tip={}, Sahip={}", request.getAccountType(), userId);
        AccountResponse response = accountService.createAccount(request, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hesap başarıyla açıldı", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal String userId) {

        List<AccountResponse> accounts = accountService.getAccountsByOwner(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Hesaplar listelendi", accounts));
    }

    /**
     * Bakiye sorgusu — Redis cache'den gelir (cache miss olursa DB).
     */
    @GetMapping("/{iban}/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(
            @PathVariable String iban,
            @AuthenticationPrincipal String userId) {

        BigDecimal balance = accountService.getBalance(iban, UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Bakiye sorgulandı", balance));
    }

    /**
     * Hesap dondurma — sadece EMPLOYEE veya ADMIN yapabilir.
     * @PreAuthorize: SecurityConfig'deki URL bazlı yetki yerine metod bazlı kontrol.
     */
    @PutMapping("/{iban}/freeze")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable String iban) {
        AccountResponse response = accountService.freezeAccount(iban);
        return ResponseEntity.ok(ApiResponse.success("Hesap donduruldu", response));
    }

    @PutMapping("/{iban}/activate")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> activateAccount(@PathVariable String iban) {
        AccountResponse response = accountService.activateAccount(iban);
        return ResponseEntity.ok(ApiResponse.success("Hesap aktifleştirildi", response));
    }
}
