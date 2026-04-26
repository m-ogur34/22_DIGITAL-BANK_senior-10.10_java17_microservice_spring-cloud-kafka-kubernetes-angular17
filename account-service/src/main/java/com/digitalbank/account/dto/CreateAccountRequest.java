package com.digitalbank.account.dto;

import com.digitalbank.account.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Yeni hesap oluşturma isteği DTO'su.
 */
@Data
public class CreateAccountRequest {

    @NotNull(message = "Hesap tipi boş olamaz")
    private AccountType accountType;

    @Size(max = 100, message = "Hesap adı en fazla 100 karakter olabilir")
    private String accountName;

    // Sadece vadeli hesap için
    private Integer vadeGunu;           // 30, 60, 90, 180, 365
    private java.math.BigDecimal faizOrani;

    // Sadece yatırım hesabı için
    private Integer riskSeviyesi;       // 1-5
}
