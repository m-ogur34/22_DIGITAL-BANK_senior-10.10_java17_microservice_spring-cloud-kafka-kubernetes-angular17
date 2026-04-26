package com.digitalbank.auth.service;

import com.digitalbank.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Refresh token temizleme servisi.
 * Periyodik olarak süresi dolmuş ve iptal edilmiş token'ları siler.
 * @Scheduled: Spring'in zamanlanmış görev mekanizması.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Her gece gece yarısı süresi dolmuş token'ları temizler.
     * cron = "0 0 0 * * *": saniye, dakika, saat, gün, ay, haftanın günü
     * Alternatif: fixedDelay ile belirli aralıkta çalıştırılabilir.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Süresi dolmuş refresh token'lar temizleniyor...");
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("Temizleme tamamlandı");
    }
}
