package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * İstenen hesap veritabanında bulunamadığında fırlatılır.
 * HTTP 404 Not Found: kaynağın var olmadığını belirtir.
 */
public class AccountNotFoundException extends DigitalBankException {

    public AccountNotFoundException(String identifier) {
        super(
            String.format("Hesap bulunamadı: %s", identifier),
            HttpStatus.NOT_FOUND,
            "ACCOUNT_NOT_FOUND"
        );
    }
}
