package com.digitalbank.loan.service;

import com.digitalbank.loan.dto.LoanApplicationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Kredi skoru hesaplama servisi (simülasyon).
 *
 * Gerçek kredi skorlama: Findeks, KKB (Kredi Kayıt Bürosu) API'leri kullanılır.
 * Bu simülasyonda gelir, mevcut kredi ve hesap yaşına göre skor hesaplanır.
 *
 * Skor aralıkları (0-1000):
 * 800-1000: Mükemmel — tüm krediler onaylanır
 * 600-799:  İyi       — standart faiz oranları
 * 400-599:  Orta      — yüksek faiz veya düşük tutar
 * 200-399:  Zayıf     — yalnızca küçük tutarlar
 * 0-199:    Kötü      — kredi reddedilir
 */
@Slf4j
@Service
public class CreditScoreService {

    private static final int MIN_APPROVED_SCORE = 400;

    /**
     * Başvurana kredi skoru hesaplar.
     *
     * Faktörler:
     * 1. Aylık gelir (maks 400 puan): Yüksek gelir → yüksek skor
     * 2. Borç/gelir oranı (maks 300 puan): Az borç → yüksek skor
     * 3. Talep edilen/gelir oranı (maks 300 puan): Makul talep → yüksek skor
     *
     * @param monthlyIncome    Aylık net gelir (TL)
     * @param existingDebt     Mevcut aylık kredi taksiti toplamı (TL)
     * @param request          Kredi başvuru bilgileri
     * @return 0-1000 arası kredi skoru
     */
    public int calculateScore(BigDecimal monthlyIncome, BigDecimal existingDebt,
                              LoanApplicationRequest request) {

        int score = 0;

        // 1. Gelir faktörü (maks 400 puan)
        // 10.000 TL altı: 100 puan, 10.000-30.000: 200 puan, 30.000+: 400 puan
        if (monthlyIncome != null) {
            if (monthlyIncome.compareTo(BigDecimal.valueOf(30_000)) >= 0) score += 400;
            else if (monthlyIncome.compareTo(BigDecimal.valueOf(10_000)) >= 0) score += 250;
            else if (monthlyIncome.compareTo(BigDecimal.valueOf(5_000)) >= 0) score += 150;
            else score += 50;
        }

        // 2. Borç/gelir oranı (maks 300 puan)
        // %30 altı borç/gelir oranı → iyi
        if (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = existingDebt.divide(monthlyIncome, 2, java.math.RoundingMode.HALF_UP);
            if (debtRatio.compareTo(BigDecimal.valueOf(0.20)) <= 0) score += 300;
            else if (debtRatio.compareTo(BigDecimal.valueOf(0.30)) <= 0) score += 200;
            else if (debtRatio.compareTo(BigDecimal.valueOf(0.50)) <= 0) score += 100;
            // %50 üzeri: 0 puan
        }

        // 3. Talep tutarı/gelir oranı (maks 300 puan)
        // Talep edilen taksit aylık gelirin %40'ını geçmemeli
        if (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal estimatedInstallment = request.getAmount()
                    .divide(BigDecimal.valueOf(request.getTermMonths()), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal installmentRatio = estimatedInstallment.divide(monthlyIncome, 2, java.math.RoundingMode.HALF_UP);

            if (installmentRatio.compareTo(BigDecimal.valueOf(0.20)) <= 0) score += 300;
            else if (installmentRatio.compareTo(BigDecimal.valueOf(0.30)) <= 0) score += 200;
            else if (installmentRatio.compareTo(BigDecimal.valueOf(0.40)) <= 0) score += 100;
            // %40 üzeri: 0 puan
        }

        // Maks 1000 ile sınırla
        score = Math.min(score, 1000);

        log.info("Kredi skoru hesaplandı: {}/1000 (gelir={}, mevcutBorç={})",
                score, monthlyIncome, existingDebt);
        return score;
    }

    /**
     * Verilen skor için kredi onaylanabilir mi?
     */
    public boolean isEligible(int score) {
        return score >= MIN_APPROVED_SCORE;
    }
}
