package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Kredi başvurusu işlenirken iş kuralı ihlali oluştuğunda fırlatılır.
 * Örn: Kredi skoru yetersiz, maksimum kredi sayısı aşıldı.
 * HTTP 422: İş kuralı ihlali.
 */
public class LoanApplicationException extends DigitalBankException {

    public LoanApplicationException(String reason) {
        super(
            String.format("Kredi başvurusu reddedildi: %s", reason),
            HttpStatus.UNPROCESSABLE_ENTITY,
            "LOAN_APPLICATION_REJECTED"
        );
    }
}
