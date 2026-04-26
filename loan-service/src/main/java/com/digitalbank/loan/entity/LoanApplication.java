package com.digitalbank.loan.entity;

import com.digitalbank.common.entity.BaseEntity;
import com.digitalbank.loan.enums.LoanStatus;
import com.digitalbank.loan.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kredi başvurusu entity'si.
 * Onaylanan başvuruya taksit planı (Installment listesi) bağlıdır.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_applications", schema = "loan_schema",
    indexes = {
        @Index(name = "idx_loan_owner_id", columnList = "owner_id"),
        @Index(name = "idx_loan_status", columnList = "status")
    }
)
public class LoanApplication extends BaseEntity {

    // Başvuran kullanıcı ID (auth-service)
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Kullanıcının IBAN'ı — onay sonrası para buraya aktarılır
    @Column(name = "owner_iban", nullable = false, length = 26)
    private String ownerIban;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    // Talep edilen kredi tutarı
    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    // Onaylanan tutar (talep edilenden düşük olabilir)
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    // Vade süresi (ay): 12, 24, 36, 60, 120
    @Column(name = "term_months", nullable = false)
    private int termMonths;

    // Yıllık faiz oranı (%)
    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualInterestRate;

    // Aylık taksit tutarı
    @Column(name = "monthly_installment", precision = 15, scale = 2)
    private BigDecimal monthlyInstallment;

    // Toplam geri ödenecek tutar (anapara + faiz + masraflar)
    @Column(name = "total_payment", precision = 15, scale = 2)
    private BigDecimal totalPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    // Kredi skoru (0-1000)
    @Column(name = "credit_score")
    private int creditScore;

    // Red nedeni
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Başvuruya ait taksit planı
    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Installment> installments = new ArrayList<>();
}
