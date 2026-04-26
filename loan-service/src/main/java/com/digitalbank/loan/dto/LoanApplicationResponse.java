package com.digitalbank.loan.dto;

import com.digitalbank.loan.enums.LoanStatus;
import com.digitalbank.loan.enums.LoanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kredi başvuru sonuç DTO'su.
 */
@Data
@Builder
public class LoanApplicationResponse {

    private UUID applicationId;
    private LoanType loanType;
    private String loanTypeName;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private int termMonths;
    private BigDecimal annualInterestRate;
    private BigDecimal monthlyInstallment;
    private BigDecimal totalPayment;
    private LoanStatus status;
    private String statusName;
    private int creditScore;
    private String rejectionReason;
    private java.time.LocalDateTime appliedAt;
}
