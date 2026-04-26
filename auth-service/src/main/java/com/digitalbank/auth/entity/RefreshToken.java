package com.digitalbank.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh token'ları veritabanında saklayan entity.
 *
 * Neden refresh token DB'de saklanır?
 * - Access token kısa ömürlü (15dk) ve stateless — doğrulamak için DB gerekmez.
 * - Refresh token uzun ömürlü (7 gün) — çalınırsa iptal edebilmeliyiz.
 *   Stateless tutulsaydı 7 gün boyunca geçerli kalırdı, iptal edemezdik.
 *
 * Bu yaklaşımla: logout → refresh token silinir → yeni access token üretilemez.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens", schema = "auth_schema",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true)
    }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Token string'i — JWT formatında
    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    // Bu token hangi kullanıcıya ait?
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Token'ın son kullanma tarihi
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    // Token iptal edildi mi? (logout veya şifre değişikliğinde true)
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    // Oluşturulma zamanı
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Token'ın süresi dolmuş mu?
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Token kullanılabilir mi? (iptal edilmemiş ve süresi dolmamış)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
