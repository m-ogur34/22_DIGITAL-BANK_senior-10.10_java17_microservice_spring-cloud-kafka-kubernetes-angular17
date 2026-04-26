package com.digitalbank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token yenileme isteği DTO'su.
 * Access token süresi dolduğunda istemci bu endpoint'i çağırır.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;
}
