package com.digitalbank.loan.service.calculator;

import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.common.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Temel kredi maliyeti hesaplayıcı.
 * Sadece anapara + faiz hesaplar, ek masraflar yok.
 * Decorator'lar bu sınıfı sararak ek masraflar ekler.
 */
@Component
public class BaseLoanCalculator implements LoanCalculator {

    /**
     * Toplam ödeme = aylık taksit × vade (ay)
     * Aylık taksit annuity formülü ile hesaplanır (MoneyUtils).
     */
    @Override
    public BigDecimal hesapla(LoanApplicationRequest request) {
        // Aylık faiz oranı (yıllık / 12)
        BigDecimal monthlyRate = request.getLoanType().getMonthlyInterestRate();

        // Aylık eşit taksit (annuity formülü)
        BigDecimal monthlyInstallment = MoneyUtils.calculateMonthlyInstallment(
                request.getAmount(),
                monthlyRate.multiply(BigDecimal.valueOf(12)), // aylık → yıllık
                request.getTermMonths()
        );

        // Toplam ödeme = taksit × ay sayısı
        return MoneyUtils.multiply(monthlyInstallment, BigDecimal.valueOf(request.getTermMonths()));
    }
}
