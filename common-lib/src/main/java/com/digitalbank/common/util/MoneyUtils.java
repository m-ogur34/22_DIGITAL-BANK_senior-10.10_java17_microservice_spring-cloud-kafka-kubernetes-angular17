package com.digitalbank.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Para işlemleri için yardımcı sınıf.
 *
 * NEDEN BigDecimal?
 * float/double IEEE 754 kayan nokta standardını kullanır ve ikili sayı
 * sisteminde bazı ondalıklı sayıları tam temsil edemez:
 *   double sonuc = 0.1 + 0.2; // 0.30000000000000004 !!!
 * Finansal işlemlerde 1 kuruş bile hata kabul edilemez.
 * BigDecimal keyfi hassasiyetle tam ondalıklı aritmetik sağlar.
 *
 * Alternatif: long (kuruş cinsinden saklamak) — bazı sistemler bunu tercih eder
 * ama gösterme aşamasında dönüşüm hatası riski taşır.
 */
public final class MoneyUtils {

    // Finansal işlemlerde standart 2 ondalık basamak (TL.KR)
    private static final int SCALE = 2;

    // HALF_UP: bankacılıkta standart yuvarlama — 0.5 yukarı yuvarlanır
    // Alternatif: HALF_EVEN (Banker's Rounding) — istatistiksel tarafsızlık için
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Türk Lirası biçimlendirme için yerel ayar
    private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");

    // Utility sınıfı: instantiation'ı engelle
    private MoneyUtils() {}

    /**
     * İki BigDecimal'ı toplar ve 2 ondalıkta yuvarlar.
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * BigDecimal'dan çıkarma işlemi yapar.
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * İki BigDecimal'ı çarpar — faiz hesaplamalarında kullanılır.
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Bakiyenin transfere yetip yetmediğini kontrol eder.
     *
     * @param balance  Mevcut bakiye
     * @param amount   Transfer tutarı
     * @return Bakiye yeterliyse true
     */
    public static boolean isSufficient(BigDecimal balance, BigDecimal amount) {
        // compareTo: BigDecimal'da == kullanmak sakıncalı (scale farklılığı)
        // compareTo 0 veya pozitif döner → bakiye >= tutar
        return balance.compareTo(amount) >= 0;
    }

    /**
     * Tutarın pozitif olup olmadığını doğrular.
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * BigDecimal'ı 2 ondalık basamağa yuvarlar.
     */
    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Tutarı Türkçe para formatında gösterir.
     * Örn: 1500.50 → "₺1.500,50"
     */
    public static String format(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(TURKISH_LOCALE);
        return formatter.format(amount);
    }

    /**
     * Aylık eşit taksit (annuity) formülü:
     * M = P * [r(1+r)^n] / [(1+r)^n - 1]
     * P = anapara, r = aylık faiz oranı, n = vade (ay)
     */
    public static BigDecimal calculateMonthlyInstallment(
            BigDecimal principal, BigDecimal annualInterestRate, int termMonths) {

        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            // Faizsiz kredi: anapara / vade
            return principal.divide(BigDecimal.valueOf(termMonths), SCALE, ROUNDING_MODE);
        }

        // Aylık faiz oranı: yıllık / 12
        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(100), 10, ROUNDING_MODE)
                .divide(BigDecimal.valueOf(12), 10, ROUNDING_MODE);

        // (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(termMonths);

        // Formülün payı: P * r * (1+r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);

        // Formülün paydası: (1+r)^n - 1
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, ROUNDING_MODE);
    }
}
