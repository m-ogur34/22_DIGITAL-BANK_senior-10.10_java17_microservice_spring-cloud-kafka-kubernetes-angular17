package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Hesap bakiyesi işlem tutarını karşılamadığında fırlatılır.
 * HTTP 422 Unprocessable Entity: istek geçerli ama işlenemez (iş kuralı ihlali).
 * Alternatif HTTP 400 olabilir ama 422 daha semantik olarak doğrudur.
 */
public class InsufficientFundsException extends DigitalBankException {

    public InsufficientFundsException(String accountIban) {
        super(
            String.format("Hesap bakiyesi yetersiz: IBAN %s", accountIban),
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INSUFFICIENT_FUNDS"
        );
    }
}
