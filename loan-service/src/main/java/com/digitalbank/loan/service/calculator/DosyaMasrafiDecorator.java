package com.digitalbank.loan.service.calculator;

import com.digitalbank.loan.dto.LoanApplicationRequest;
import com.digitalbank.common.util.MoneyUtils;

import java.math.BigDecimal;

/**
 * Dosya ve işlem masrafı ekleyen Decorator.
 *
 * Sabit masraflar:
 * - Ekspertiz ücreti: 500 TL (konut için)
 * - Dosya masrafı: kredi tutarının %1'i (min 250, maks 2000 TL)
 * - BSMV: Faizin %15'i (vergi)
 *
 * Kullanım:
 * new DosyaMasrafiDecorator(new SigortaDecorator(new BaseLoanCalculator()))
 * → Önce base, sigorta ekle, sonra dosya masrafı ekle
 */
public class DosyaMasrafiDecorator extends LoanCalculatorDecorator {

    private static final BigDecimal MIN_DOSYA_MASRAFI = new BigDecimal("250.00");
    private static final BigDecimal MAX_DOSYA_MASRAFI = new BigDecimal("2000.00");
    private static final BigDecimal DOSYA_MASRAFI_ORANI = new BigDecimal("0.01"); // %1

    public DosyaMasrafiDecorator(LoanCalculator wrapped) {
        super(wrapped);
    }

    @Override
    public BigDecimal hesapla(LoanApplicationRequest request) {
        BigDecimal baseTotal = wrapped.hesapla(request);

        // Dosya masrafı: Tutar × %1, fakat min 250 maks 2000 TL
        BigDecimal dosyaMasrafi = MoneyUtils.multiply(request.getAmount(), DOSYA_MASRAFI_ORANI);
        dosyaMasrafi = dosyaMasrafi.max(MIN_DOSYA_MASRAFI).min(MAX_DOSYA_MASRAFI);

        // Konut kredisi için ek ekspertiz ücreti
        if (request.getLoanType() == com.digitalbank.loan.enums.LoanType.KONUT) {
            dosyaMasrafi = MoneyUtils.add(dosyaMasrafi, new BigDecimal("500.00"));
        }

        return MoneyUtils.add(baseTotal, dosyaMasrafi);
    }
}
