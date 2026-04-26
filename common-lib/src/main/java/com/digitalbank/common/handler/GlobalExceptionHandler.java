package com.digitalbank.common.handler;

import com.digitalbank.common.dto.ApiResponse;
import com.digitalbank.common.exception.DigitalBankException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Merkezi hata yönetimi sınıfı.
 *
 * @RestControllerAdvice: Tüm @RestController sınıflarındaki exception'ları yakalar.
 * Bu sayede her controller'da try-catch yazmak zorunda kalmayız.
 *
 * Mimari not: Bu sınıf common-lib'de tanımlı. Her servis bunu import ederek kullanır.
 * Her servisin kendi @ComponentScan'ı bu paketi taramalıdır.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Uygulamamızın özel exception'larını yakalar.
     * Her DigitalBankException kendi HTTP status kodunu taşır.
     */
    @ExceptionHandler(DigitalBankException.class)
    public ResponseEntity<ApiResponse<Void>> handleDigitalBankException(DigitalBankException ex) {
        log.error("İş kuralı hatası [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    /**
     * Bean Validation (@Valid) hatalarını yakalar.
     * Her alan için hata mesajı map'e eklenir.
     * Örn: { "email": "Geçerli bir email adresi giriniz", "password": "Şifre en az 8 karakter olmalıdır" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // Her alan hatası için alan adı → hata mesajı eşlemesi
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Doğrulama hatası: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Giriş doğrulama hatası")
                        .data(errors)
                        .errorCode("VALIDATION_ERROR")
                        .build());
    }

    /**
     * Spring Security yetkilendirme hatası — 403 Forbidden.
     * Kullanıcı giriş yapmış ama yeterli yetkisi yok.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Yetkisiz erişim girişimi: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bu işlem için yetkiniz bulunmamaktadır", "ACCESS_DENIED"));
    }

    /**
     * Kimlik doğrulama hatası — 401 Unauthorized.
     * Yanlış kullanıcı adı/şifre gibi durumlar.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Hatalı kimlik bilgileri: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email veya şifre hatalı", "BAD_CREDENTIALS"));
    }

    /**
     * Beklenmeyen tüm hataları yakalar.
     * Detayları loglar ama istemciye gizli bilgi vermez.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Beklenmeyen sistem hatası: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Sistem hatası oluştu. Lütfen daha sonra tekrar deneyiniz.", "INTERNAL_ERROR"));
    }
}
