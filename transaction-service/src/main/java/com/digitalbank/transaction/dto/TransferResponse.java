package com.digitalbank.transaction.dto;

import com.digitalbank.transaction.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Transfer işlemi yanıt DTO'su.
 */
@Data
@Builder
public class TransferResponse {

    private String transactionId;
    private String referenceId;        // Debit + Credit'i bağlayan referans
    private TransactionStatus status;
    private String message;
    private BigDecimal amount;
    private String senderIban;
    private String receiverIban;
    private java.time.LocalDateTime processedAt;
}
