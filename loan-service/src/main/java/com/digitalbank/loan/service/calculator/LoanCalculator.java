package com.digitalbank.loan.service.calculator;

import com.digitalbank.loan.dto.LoanApplicationRequest;

import java.math.BigDecimal;

/**
 * Kredi maliyet hesaplama arayüzü — Decorator Pattern temel arayüzü.
 *
 * Decorator Pattern:
 * Temel işlevi (BaseLoanCalculator) değiştirmeden
 * ek özellikler (sigorta, dosya masrafı) ekleyebiliriz.
 *
 * Kullanım:
 * LoanCalculator calc = new DosyaMasrafiDecorator(
 *                           new SigortaDecorator(
 *                               new BaseLoanCalculator()));
 * BigDecimal toplam = calc.hesapla(request);
 *
 * Open/Closed Principle: Yeni masraf türü için sadece yeni Decorator yazılır,
 * mevcut kod değişmez.
 */
public interface LoanCalculator {

    /**
     * Kredi toplam maliyetini hesaplar.
     *
     * @param request  Kredi başvuru bilgileri
     * @return Toplam ödeme tutarı (anapara + faiz + masraflar)
     */
    BigDecimal hesapla(LoanApplicationRequest request);
}
