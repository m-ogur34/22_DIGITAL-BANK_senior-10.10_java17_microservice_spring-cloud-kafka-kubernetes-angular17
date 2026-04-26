package com.digitalbank.loan.service.calculator;

import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.loan.enums.LoanType;
import com.digitalbank.common.util.MoneyUtils;

import java.math.BigDecimal;

/**
 * Sigorta masrafı ekleyen Decorator.
 *
 * Zorunlu sigorta türleri:
 * - İhtiyaç: Hayat sigortası %0.3 (aylık taksit üzerinden)
 * - Konut: DASK + hayat sigortası %0.5
 * - Taşıt: Kasko sigortası %0.4
 *
 * Bu masraf toplam ödemeye eklenir.
 */
public class SigortaDecorator extends LoanCalculatorDecorator {

    public SigortaDecorator(LoanCalculator wrapped) {
        super(wrapped);
    }

    @Override
    public BigDecimal hesapla(LoanApplicationRequest request) {
        // Temel hesaplamayı al
        BigDecimal baseTotal = wrapped.hesapla(request);

        // Sigorta oranı: kredi türüne göre belirlenir
        BigDecimal sigortaOrani = getSigortaOrani(request.getLoanType());

        // Sigorta masrafı = toplam × sigorta oranı
        BigDecimal sigortaMasrafi = MoneyUtils.multiply(baseTotal, sigortaOrani);

        return MoneyUtils.add(baseTotal, sigortaMasrafi);
    }

    private BigDecimal getSigortaOrani(LoanType type) {
        return switch (type) {
            case IHTIYAC -> new BigDecimal("0.003"); // %0.3
            case KONUT   -> new BigDecimal("0.005"); // %0.5
            case TASIT   -> new BigDecimal("0.004"); // %0.4
        };
    }
}
