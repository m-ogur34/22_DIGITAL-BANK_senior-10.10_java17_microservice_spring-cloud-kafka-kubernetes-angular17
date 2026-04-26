package com.digitalbank.common.security;

import com.digitalbank.common.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT (JSON Web Token) üretme ve doğrulama yardımcı sınıfı.
 *
 * Bu sınıf tüm servisler tarafından common-lib üzerinden paylaşılır.
 * Her servis kendi Spring Security filter chain'ini kurar ama token
 * doğrulama mantığı burada merkezileştirilir.
 *
 * JWT Yapısı:
 * - Header: algoritma bilgisi (HS256)
 * - Payload (Claims): sub (userId), email, roles, iat, exp
 * - Signature: header + payload'ın secret key ile HMAC-SHA256 imzası
 *
 * Neden HMAC-SHA256? Asimetrik RSA'ya göre daha hızlı ve dahili servisler
 * için yeterince güvenli. Public-facing API'lerde RSA tercih edilebilir.
 */
@Slf4j
@Component
public class JwtUtil {

    // JWT imzalama için gizli anahtar — Base64 encoded, env'den gelir
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Access token geçerlilik süresi (milisaniye) — varsayılan: 15 dakika
    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    // Refresh token geçerlilik süresi (milisaniye) — varsayılan: 7 gün
    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    /**
     * Secret key'i güvenli SecretKey nesnesine dönüştürür.
     * JJWT 0.12.x API'sinde Keys.hmacShaKeyFor() kullanılır.
     * Base64 decode edilerek byte[] formatına çevrilir.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access token üretir.
     *
     * @param userId  Kullanıcının UUID'si — token'ın "subject"i
     * @param email   Kullanıcının email adresi — claim olarak eklenir
     * @param roles   Kullanıcının rolleri — yetkilendirme kararları için
     * @return İmzalanmış JWT string'i
     */
    public String generateAccessToken(String userId, String email, List<String> roles) {
        return buildToken(userId, email, roles, accessTokenExpiration);
    }

    /**
     * Refresh token üretir. Access token'a göre daha uzun geçerlilik süresi.
     * Refresh token PostgreSQL'de de saklanır (auth-service'in refresh_tokens tablosu).
     */
    public String generateRefreshToken(String userId, String email, List<String> roles) {
        return buildToken(userId, email, roles, refreshTokenExpiration);
    }

    /**
     * JWT token'ı oluşturur.
     * Claims: taşınan veriler — standart (sub, iat, exp) + özel (email, roles)
     */
    private String buildToken(String userId, String email, List<String> roles, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                // subject: token sahibinin benzersiz kimliği
                .subject(userId)
                // Özel claim'ler: servisler bu bilgileri SecurityContext'e aktarır
                .claims(Map.of(
                    "email", email,
                    "roles", roles,
                    // jti (JWT ID): her token için benzersiz ID — blacklist kontrolünde kullanılır
                    "jti", UUID.randomUUID().toString()
                ))
                .issuedAt(now)
                .expiration(expiryDate)
                // HS256 algoritması ile imzala
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Token'dan kullanıcı ID'sini (subject) çıkarır.
     * Token geçersizse InvalidTokenException fırlatır.
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Token'dan email'i çıkarır.
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Token'dan rolleri çıkarır.
     * Roles JSON array olarak saklanır, List<String> olarak döner.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * Token'ın JTI (JWT ID) değerini çıkarır.
     * Blacklist kontrolünde bu ID kullanılır.
     */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    /**
     * Token'ın bitiş zamanını döner.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Token'dan herhangi bir claim'i çıkarmak için generic metod.
     * Function<Claims, T> ile hangi alanın istendiği belirtilir.
     * Örn: extractClaim(token, Claims::getSubject)
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Token'ı parse edip tüm claim'leri döner.
     * Geçersiz/süresi dolmuş token'da exception fırlatır.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token süresi dolmuş: {}", e.getMessage());
            throw new InvalidTokenException("Token süresi dolmuş");
        } catch (MalformedJwtException e) {
            log.warn("Hatalı JWT formatı: {}", e.getMessage());
            throw new InvalidTokenException("Token formatı hatalı");
        } catch (SecurityException e) {
            log.warn("JWT imza doğrulama hatası: {}", e.getMessage());
            throw new InvalidTokenException("Token imzası geçersiz");
        } catch (JwtException e) {
            log.warn("JWT işleme hatası: {}", e.getMessage());
            throw new InvalidTokenException("Token işlenemedi");
        }
    }

    /**
     * Token'ın geçerli olup olmadığını kontrol eder.
     * Sadece format ve imza kontrolü yapar; blacklist kontrolü serviste yapılır.
     *
     * @param token  Doğrulanacak JWT string'i
     * @return Token geçerliyse true, değilse false
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (InvalidTokenException e) {
            return false;
        }
    }

    /**
     * Token'ın süresinin dolup dolmadığını kontrol eder.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (InvalidTokenException e) {
            return true;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
