package com.digitalbank.transaction.dto;

import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * İşlem geçmişi arama/filtreleme parametreleri.
 * Elasticsearch'e gönderilecek query'yi oluşturmak için kullanılır.
 */
@Data
public class TransactionSearchRequest {

    // Açıklama alanında full-text arama
    private String keyword;

    // IBAN filtreleme
    private String iban;

    // Tarih aralığı filtreleme
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // Tutar aralığı filtreleme
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // Durum filtresi
    private TransactionStatus status;

    // Tip filtresi
    private TransactionType type;

    // Sayfalama
    private int page = 0;
    private int size = 20;
}
