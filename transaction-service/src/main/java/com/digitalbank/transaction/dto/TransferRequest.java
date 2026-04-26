package com.digitalbank.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Para transfer isteği DTO'su.
 */
@Data
public class TransferRequest {

    @NotBlank(message = "Gönderen IBAN boş olamaz")
    @Size(min = 26, max = 26, message = "IBAN 26 karakter olmalıdır")
    private String senderIban;

    @NotBlank(message = "Alıcı IBAN boş olamaz")
    @Size(min = 26, max = 26, message = "IBAN 26 karakter olmalıdır")
    private String receiverIban;

    @NotNull(message = "Tutar boş olamaz")
    @DecimalMin(value = "0.01", message = "Transfer tutarı 0'dan büyük olmalıdır")
    @DecimalMax(value = "999999999.99", message = "Transfer tutarı çok yüksek")
    private BigDecimal amount;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir")
    private String description;

    // İşlemi yapan kullanıcının ID'si (SecurityContext'ten alınır, body'de gelmez)
    private String ownerId;

    // Mevcut bakiye (account-service'den doğrulanır)
    private BigDecimal currentBalance;
}
