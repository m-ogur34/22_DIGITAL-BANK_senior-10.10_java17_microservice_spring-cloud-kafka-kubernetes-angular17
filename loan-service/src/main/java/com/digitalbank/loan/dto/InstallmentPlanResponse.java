package com.digitalbank.loan.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Taksit planı yanıt DTO'su.
 */
@Data
@Builder
public class InstallmentPlanResponse {

    private UUID loanApplicationId;
    private BigDecimal totalAmount;
    private BigDecimal monthlyInstallment;
    private int termMonths;
    private List<InstallmentDto> installments;

    @Data
    @Builder
    public static class InstallmentDto {
        private int number;
        private LocalDate dueDate;
        private BigDecimal amount;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private BigDecimal remainingPrincipal;
        private boolean paid;
        private LocalDate paymentDate;
    }
}
