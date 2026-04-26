package com.digitalbank.loan.controller;

import com.digitalbank.loan.dto.InstallmentPlanResponse;
import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.loan.dto.LoanApplicationResponse;
import com.digitalbank.loan.service.LoanService;
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
 * Kredi endpoint'leri.
 *
 * POST /loans/apply             → Kredi başvurusu
 * GET  /loans                   → Başvurularım
 * GET  /loans/{id}/installments → Taksit planı
 */
@Slf4j
@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> apply(
            @Valid @RequestBody LoanApplicationRequest request,
            @AuthenticationPrincipal String userId,
            // Gerçekte auth-service'den kullanıcı bilgisi HTTP ile alınır
            // Simülasyon: header'dan aylık gelir
            @RequestHeader(value = "X-Monthly-Income", defaultValue = "10000") BigDecimal monthlyIncome) {

        log.info("Kredi başvurusu: tip={}, tutar={}, userId={}", request.getLoanType(), request.getAmount(), userId);
        LoanApplicationResponse response = loanService.apply(request, UUID.fromString(userId), monthlyIncome);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Başvuru alındı", response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponse>>> getMyLoans(
            @AuthenticationPrincipal String userId) {
        List<LoanApplicationResponse> loans = loanService.getMyLoans(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Krediler listelendi", loans));
    }

    @GetMapping("/{loanId}/installments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InstallmentPlanResponse>> getInstallmentPlan(
            @PathVariable UUID loanId) {
        InstallmentPlanResponse plan = loanService.getInstallmentPlan(loanId);
        return ResponseEntity.ok(ApiResponse.success("Taksit planı", plan));
    }
}
