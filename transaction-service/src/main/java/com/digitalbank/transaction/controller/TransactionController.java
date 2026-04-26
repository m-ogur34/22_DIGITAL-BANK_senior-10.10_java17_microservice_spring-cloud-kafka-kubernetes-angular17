package com.digitalbank.transaction.controller;

import com.digitalbank.transaction.document.TransactionDocument;
import com.digitalbank.transaction.dto.TransactionSearchRequest;
import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.service.TransactionService;
import com.digitalbank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transfer ve işlem geçmişi endpoint'leri.
 *
 * POST /transactions/transfer          → Para gönder
 * GET  /transactions                   → İşlem geçmişim (DB'den, sayfalı)
 * GET  /transactions/search            → Elasticsearch arama
 * POST /transactions/{ref}/reverse     → Transfer iptal (ADMIN)
 */
@Slf4j
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Para transferi.
     * @AuthenticationPrincipal: JWT'den gelen userId (filter'da set edildi).
     */
    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal String userId) {

        log.info("Transfer isteği: {} → {} ({})", request.getSenderIban(), request.getReceiverIban(), userId);
        TransferResponse response = transactionService.transfer(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Transfer başarılı", response));
    }

    /**
     * Sayfalı işlem geçmişi (PostgreSQL'den).
     * @RequestParam: URL query parametresi — ?page=0&size=20
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Transaction>>> getHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Transaction> history = transactionService.getTransactionHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("İşlem geçmişi", history));
    }

    /**
     * Elasticsearch ile işlem arama.
     * Keyword, tarih, tutar, IBAN filtreleme.
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TransactionDocument>>> search(
            TransactionSearchRequest searchRequest,
            @AuthenticationPrincipal String userId) {

        List<TransactionDocument> results = transactionService.searchTransactions(searchRequest, userId);
        return ResponseEntity.ok(ApiResponse.success("Arama sonuçları", results));
    }

    /**
     * Transfer geri alma — sadece ADMIN.
     */
    @PostMapping("/{referenceId}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reverseTransaction(
            @PathVariable String referenceId) {

        transactionService.reverseTransaction(referenceId);
        return ResponseEntity.ok(ApiResponse.success("Transfer iptal edildi"));
    }
}
