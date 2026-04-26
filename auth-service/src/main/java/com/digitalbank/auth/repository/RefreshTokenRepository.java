package com.digitalbank.auth.repository;

import com.digitalbank.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token veritabanı işlemleri.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Token string'ine göre refresh token bulur — token yenileme işleminde kullanılır.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Kullanıcının tüm geçerli refresh token'larını iptal eder.
     * @Modifying: UPDATE/DELETE sorgusu olduğunu belirtir — zorunlu.
     * clearAutomatically = true: 1. seviye cache'i temizler, güncel veri okunur.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    void revokeAllByUserId(UUID userId);

    /**
     * Süresi dolmuş token'ları siler — periyodik temizlik için (scheduled job).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens(LocalDateTime now);
}
