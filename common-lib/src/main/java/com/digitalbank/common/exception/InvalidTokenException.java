package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT token geçersiz, süresi dolmuş veya blacklist'te olduğunda fırlatılır.
 * HTTP 401 Unauthorized: kimlik doğrulama başarısız.
 */
public class InvalidTokenException extends DigitalBankException {

    public InvalidTokenException(String reason) {
        super(
            String.format("Geçersiz token: %s", reason),
            HttpStatus.UNAUTHORIZED,
            "INVALID_TOKEN"
        );
    }
}
