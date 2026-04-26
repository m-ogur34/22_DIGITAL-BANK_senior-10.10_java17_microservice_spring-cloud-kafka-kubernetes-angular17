package com.digitalbank.common.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

/**
 * Günlük işlem limiti aşıldığında fırlatılır.
 * HTTP 429 Too Many Requests: rate-limit / limit aşımı için semantik olarak doğru.
 * Alternatif: 422 kullanılabilir ama 429 daha açıklayıcıdır.
 */
public class DailyLimitExceededException extends DigitalBankException {

    public DailyLimitExceededException(BigDecimal limit) {
        super(
            String.format("Günlük işlem limiti aşıldı. Limit: %s TL", limit.toPlainString()),
            HttpStatus.TOO_MANY_REQUESTS,
            "DAILY_LIMIT_EXCEEDED"
        );
    }
}
