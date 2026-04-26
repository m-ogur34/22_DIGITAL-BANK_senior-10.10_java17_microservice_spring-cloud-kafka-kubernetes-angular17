package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

/**
 * Tüm DigitalBank özel exception'larının temel sınıfı.
 *
 * RuntimeException'dan kalıtım alıyoruz çünkü:
 * - Checked exception'lar (Exception) her metotta throws bildirimi gerektirir → boilerplate
 * - Spring @Transactional yalnızca RuntimeException'da rollback yapar (varsayılan)
 * - Modern Spring uygulamalarında unchecked exception tercih edilir
 *
 * Her exception HTTP status kodu taşıyor: bu sayede GlobalExceptionHandler
 * doğrudan bu kodu kullanabilir.
 */
@Getter
public class DigitalBankException extends RuntimeException {

    // HTTP yanıt kodu: 400, 404, 409, 422 vb.
    private final HttpStatus httpStatus;

    // Makine tarafından işlenebilir hata kodu: "INSUFFICIENT_FUNDS", "ACCOUNT_NOT_FOUND" vb.
    private final String errorCode;

    public DigitalBankException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public DigitalBankException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
