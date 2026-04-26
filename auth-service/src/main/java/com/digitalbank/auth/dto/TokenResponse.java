package com.digitalbank.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Başarılı giriş/token yenileme işleminin yanıt DTO'su.
 * Hem access token hem refresh token döner.
 *
 * Güvenlik notu: Refresh token HTTP-only cookie'de saklanmalıdır
 * (XSS saldırılarına karşı). Bu projede basitlik için body'de döndürülüyor.
 * Prodüksiyon için cookie yaklaşımı tercih edilir.
 */
@Data
@Builder
public class TokenResponse {

    // Kısa ömürlü (15dk) — API isteklerinde kullanılır
    private String accessToken;

    // Uzun ömürlü (7 gün) — yeni access token almak için kullanılır
    private String refreshToken;

    // Token tipi — HTTP Authorization header formatı için: "Bearer <token>"
    @Builder.Default
    private String tokenType = "Bearer";

    // Access token'ın kaç saniye sonra dolacağı (istemci yenileme zamanlaması için)
    private long expiresIn;

    // Kullanıcının rolleri — frontend'in menü/yetki kararı için
    private List<String> roles;

    // Kullanıcı email'i — hoşgeldin mesajı vb. için
    private String email;

    // Kullanıcının tam adı
    private String fullName;
}
