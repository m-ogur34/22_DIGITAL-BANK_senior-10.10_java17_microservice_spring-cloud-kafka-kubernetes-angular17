package com.digitalbank.account.dto;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hesap bilgileri yanıt DTO'su.
 * Tam entity yerine sadece gerekli alanları istemciye döndürürüz (bilgi güvenliği).
 */
@Data
@Builder
public class AccountResponse {

    private UUID id;
    private String iban;
    private BigDecimal balance;
    private AccountType accountType;
    private String accountTypeName;     // "Vadesiz Hesap" gibi okunabilir isim
    private AccountStatus status;
    private String statusName;          // "Aktif" gibi
    private String accountName;
    private String currency;
    private UUID ownerId;
    private LocalDateTime createdAt;

    // Vadeli hesap ek alanları
    private Integer vadeGunu;
    private BigDecimal faizOrani;
    private java.time.LocalDate vadeBitis;

    // Yatırım hesabı ek alanları
    private Integer riskSeviyesi;
    private BigDecimal portfoyDegeri;
}
