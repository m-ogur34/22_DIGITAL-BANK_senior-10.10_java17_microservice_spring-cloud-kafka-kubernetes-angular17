package com.digitalbank.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Kullanıcı kayıt isteği DTO'su.
 * @Data: Lombok — getter, setter, equals, hashCode, toString otomatik üretir.
 * Bean Validation anotasyonları ile istemci verisi doğrulanır.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Ad boş olamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;

    @NotBlank(message = "TC Kimlik No boş olamaz")
    @Size(min = 11, max = 11, message = "TC Kimlik No 11 haneli olmalıdır")
    @Pattern(regexp = "\\d{11}", message = "TC Kimlik No yalnızca rakamlardan oluşmalıdır")
    private String tcNo;

    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    private String password;

    @Pattern(regexp = "^(\\+90|0)?[0-9]{10}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;

    @DecimalMin(value = "0.00", message = "Aylık gelir negatif olamaz")
    private BigDecimal monthlyIncome;
}
