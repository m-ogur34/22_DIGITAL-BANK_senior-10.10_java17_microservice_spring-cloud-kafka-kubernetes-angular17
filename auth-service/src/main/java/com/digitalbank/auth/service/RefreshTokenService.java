package com.digitalbank.auth.service;

import com.digitalbank.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * REFRESH TOKEN TEMİZLEME SERVİSİ — Zamanlanmış Görev
 * =====================================================
 *
 * Neden periyodik temizleme gerekli?
 *   Her login veya token yenileme işlemi DB'ye yeni bir refresh token kaydı ekler.
 *   İptal edilmiş (revoked=true) veya süresi dolmuş token'lar DB'de birikir.
 *   Birikim: 10.000 kullanıcı × 30 login/ay = 300.000 satır/ay → performans düşüşü.
 *   Temizleme: Artık hiçbir zaman kullanılmayacak kayıtları periyodik olarak sil.
 *
 * @Scheduled nedir?
 *   Spring'in yerleşik zamanlanmış görev mekanizması.
 *   @EnableScheduling: Uygulama başlatıcısında etkinleştirilmeli.
 *   Alternatif: Quartz Scheduler (daha karmaşık, clustered ortam için), AWS EventBridge, Kubernetes CronJob.
 *   Basit kullanım için @Scheduled yeterli — tek JVM instance'ı varsa uygundur.
 *   Dikkat: Birden fazla pod (Kubernetes) varsa aynı job N kez çalışır.
 *   Çözüm: ShedLock veya Quartz ile distributed lock kullanılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Her gece yarısı süresi dolmuş ve iptal edilmiş token'ları temizler.
     *
     * cron ifadesi: "0 0 0 * * *"
     *   Format: saniye dakika saat gün-ay ay gün-hafta
     *   0       → saniye: tam saniyede
     *   0       → dakika: tam dakikada (0. dakika)
     *   0       → saat: gece yarısı (00:00:00)
     *   *       → ayın her günü
     *   *       → her ay
     *   *       → haftanın her günü
     *   Sonuç: Her gece saat 00:00:00'da çalışır.
     *
     * Neden gece yarısı?
     *   En düşük trafik saati → DB yüküne en az etkili zaman.
     *   Gündüz batch delete yapılsaydı aktif kullanıcı sorgularıyla çakışabilirdi.
     *
     * @Transactional neden gerekli?
     *   deleteExpiredAndRevokedTokens(): Birden fazla satırı silen toplu DELETE.
     *   Transaction: Silme sırasında hata olursa kısmen silinmiş veri kalmaz.
     *   Tüm silme işlemi atomik — başarılı ya da hiçbiri.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Süresi dolmuş refresh token'lar temizleniyor...");
        // LocalDateTime.now(): Bu anı referans alır — bundan önce süresi dolmuş + revoked token'lar silinir
        // SQL: DELETE FROM refresh_tokens WHERE (expiry_date < now) OR (revoked = true)
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("Refresh token temizleme tamamlandı");
    }
}
