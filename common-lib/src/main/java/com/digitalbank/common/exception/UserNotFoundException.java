package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Kullanıcı veritabanında bulunamadığında fırlatılır.
 * HTTP 404 Not Found.
 */
public class UserNotFoundException extends DigitalBankException {

    public UserNotFoundException(String identifier) {
        super(
            String.format("Kullanıcı bulunamadı: %s", identifier),
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND"
        );
    }
}
