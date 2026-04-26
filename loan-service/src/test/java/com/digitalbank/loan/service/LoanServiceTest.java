package com.digitalbank.loan.service;

import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.loan.dto.LoanApplicationResponse;
import com.digitalbank.loan.entity.LoanApplication;
import com.digitalbank.loan.enums.LoanStatus;
import com.digitalbank.loan.enums.LoanType;
import com.digitalbank.loan.kafka.LoanEventProducer;
import com.digitalbank.loan.repository.InstallmentRepository;
import com.digitalbank.loan.repository.LoanApplicationRepository;
import com.digitalbank.loan.service.calculator.BaseLoanCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Birim Testleri")
class LoanServiceTest {

    @Mock private LoanApplicationRepository loanRepo;
    @Mock private InstallmentRepository installmentRepo;
    @Mock private CreditScoreService creditScoreService;
    @Mock private LoanEventProducer eventProducer;

    @InjectMocks
    private LoanService loanService;

    private LoanApplicationRequest validRequest;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        // BaseLoanCalculator'ı gerçek kullanmak için inject edilmeli
        var baseLoanCalculator = new BaseLoanCalculator();
        // Reflection ile inject
        try {
            var field = LoanService.class.getDeclaredField("baseLoanCalculator");
            field.setAccessible(true);
            field.set(loanService, baseLoanCalculator);
        } catch (Exception e) { /* test setup */ }

        validRequest = new LoanApplicationRequest();
        validRequest.setLoanType(LoanType.IHTIYAC);
        validRequest.setAmount(new BigDecimal("30000.00"));
        validRequest.setTermMonths(24);
        validRequest.setDisbursementIban("TR330006100012345678901234");
        validRequest.setSigortaIsteniyor(true);

        ownerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Yüksek kredi skoruyla başvuru onaylanmalı")
    void apply_withHighCreditScore_shouldBeApproved() {
        // GIVEN
        when(loanRepo.countByOwnerIdAndStatusIn(any(), anyList())).thenReturn(0L);
        when(creditScoreService.calculateScore(any(), any(), any())).thenReturn(750);
        when(creditScoreService.isEligible(750)).thenReturn(true);

        LoanApplication mockLoan = LoanApplication.builder()
                .ownerId(ownerId)
                .loanType(LoanType.IHTIYAC)
                .requestedAmount(new BigDecimal("30000.00"))
                .approvedAmount(new BigDecimal("30000.00"))
                .termMonths(24)
                .annualInterestRate(new BigDecimal("30.00"))
                .monthlyInstallment(new BigDecimal("1665.30"))
                .totalPayment(new BigDecimal("39967.20"))
                .status(LoanStatus.APPROVED)
                .creditScore(750)
                .build();
        mockLoan.setId(UUID.randomUUID());

        when(loanRepo.save(any())).thenReturn(mockLoan);
        when(installmentRepo.saveAll(anyList())).thenReturn(List.of());
        doNothing().when(eventProducer).publishLoanApproved(any());

        // WHEN
        LoanApplicationResponse response = loanService.apply(validRequest, ownerId, new BigDecimal("20000"));

        // THEN
        assertThat(response.getStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(response.getCreditScore()).isEqualTo(750);
        verify(eventProducer, times(1)).publishLoanApproved(any());
    }

    @Test
    @DisplayName("Düşük kredi skoruyla başvuru reddedilmeli")
    void apply_withLowCreditScore_shouldBeRejected() {
        // GIVEN
        when(loanRepo.countByOwnerIdAndStatusIn(any(), anyList())).thenReturn(0L);
        when(creditScoreService.calculateScore(any(), any(), any())).thenReturn(250);
        when(creditScoreService.isEligible(250)).thenReturn(false);

        LoanApplication rejectedLoan = LoanApplication.builder()
                .ownerId(ownerId)
                .loanType(LoanType.IHTIYAC)
                .requestedAmount(new BigDecimal("30000.00"))
                .status(LoanStatus.REJECTED)
                .creditScore(250)
                .rejectionReason("Kredi skoru yetersiz: 250/1000")
                .termMonths(24)
                .build();
        rejectedLoan.setId(UUID.randomUUID());

        when(loanRepo.save(any())).thenReturn(rejectedLoan);
        doNothing().when(eventProducer).publishLoanRejected(any());

        // WHEN
        LoanApplicationResponse response = loanService.apply(validRequest, ownerId, new BigDecimal("3000"));

        // THEN
        assertThat(response.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(response.getRejectionReason()).isNotBlank();
        verify(eventProducer, times(1)).publishLoanRejected(any());
        verify(installmentRepo, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Maksimum aktif kredi sayısı aşılınca hata fırlatılmalı")
    void apply_whenMaxActiveLoansReached_shouldThrowException() {
        // GIVEN — kullanıcının zaten 3 aktif kredisi var
        when(loanRepo.countByOwnerIdAndStatusIn(any(), anyList())).thenReturn(3L);

        // WHEN & THEN
        assertThatThrownBy(() -> loanService.apply(validRequest, ownerId, new BigDecimal("20000")))
                .isInstanceOf(com.digitalbank.common.exception.LoanApplicationException.class)
                .hasMessageContaining("Maksimum aktif kredi");
    }
}
