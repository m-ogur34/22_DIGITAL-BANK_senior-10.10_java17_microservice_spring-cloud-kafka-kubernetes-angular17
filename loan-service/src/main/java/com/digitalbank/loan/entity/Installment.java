package com.digitalbank.loan.entity;

import com.digitalbank.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Kredi taksit planı — her taksit bir satır.
 * Aylık eşit taksit (annuity) formülü ile hesaplanır.
 *
 * Taksit = Anapara payı + Faiz payı
 * İlk taksitlerde faiz payı yüksek, son taksitlerde anapara payı yüksek.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "installments", schema = "loan_schema",
    indexes = {
        @Index(name = "idx_installment_loan_id", columnList = "loan_application_id"),
        @Index(name = "idx_installment_due_date", columnList = "due_date")
    }
)
public class Installment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    // Taksit numarası: 1, 2, 3...
    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    // Son ödeme tarihi
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Toplam taksit tutarı (sabit)
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Bu taksitteki anapara payı
    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    // Bu taksitteki faiz payı
    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    // Ödendi mi?
    @Column(name = "is_paid", nullable = false)
    private boolean paid = false;

    // Ödeme tarihi (ödenirse)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    // Kalan anapara (bu taksit ödendikten sonra)
    @Column(name = "remaining_principal", precision = 15, scale = 2)
    private BigDecimal remainingPrincipal;
}
