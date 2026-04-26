package com.digitalbank.loan.service.calculator;

import com.digitalbank.loan.dto.LoanApplicationRequest;

import java.math.BigDecimal;

/**
 * Soyut Decorator sınıfı.
 * Tüm decorator'lar bu sınıfı extend eder.
 * Wrapped calculator'ı tutar ve hesapla() çağrısını ona delege eder + kendi eklentisini yapar.
 */
public abstract class LoanCalculatorDecorator implements LoanCalculator {

    // Sarılan (decorated) calculator
    protected final LoanCalculator wrapped;

    protected LoanCalculatorDecorator(LoanCalculator wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public BigDecimal hesapla(LoanApplicationRequest request) {
        // Alt sınıf override etmezse wrapped'i çağır
        return wrapped.hesapla(request);
    }
}
