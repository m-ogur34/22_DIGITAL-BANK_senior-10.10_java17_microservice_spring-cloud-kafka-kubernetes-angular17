package com.digitalbank.loan.dto;

import com.digitalbank.loan.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Kredi başvurusu isteği DTO'su.
 */
@Data
public class LoanApplicationRequest {

    @NotNull(message = "Kredi türü boş olamaz")
    private LoanType loanType;

    @NotNull(message = "Kredi tutarı boş olamaz")
    @DecimalMin(value = "1000.00", message = "Minimum kredi tutarı 1.000 TL'dir")
    private BigDecimal amount;

    @NotNull(message = "Vade süresi boş olamaz")
    @Min(value = 3, message = "Minimum vade 3 aydır")
    @Max(value = 240, message = "Maksimum vade 240 aydır")
    private Integer termMonths;

    // Para çekileceği IBAN
    @NotBlank(message = "IBAN boş olamaz")
    @Size(min = 26, max = 26)
    private String disbursementIban;

    // Sigorta isteniyor mu? (opsiyonel ama önerilir)
    private boolean sigortaIsteniyor = true;
}
