package com.digitalbank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Kullanıcı giriş isteği DTO'su.
 * Email + şifre kombinasyonu ile kimlik doğrulama yapılır.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    private String password;
}
