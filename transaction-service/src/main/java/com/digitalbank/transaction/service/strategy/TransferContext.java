package com.digitalbank.transaction.service.strategy;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Transfer stratejisi seçici (Context + Factory).
 *
 * Strategy Pattern'ın Context sınıfı:
 * - Hangi stratejinin kullanılacağını belirler
 * - Stratejiyi çağırır
 * - İstemci kodu (TransactionService) hangi strateji kullanıldığını bilmez
 *
 * Factory mantığı: IBAN prefix'ine bakarak strateji seçilir.
 * TR prefix'i → aynı banka içi → InternalTransferStrategy
 * Diğer prefix (DE, FR...) → banka dışı → ExternalTransferStrategy
 *
 * Open/Closed Principle: Yeni strateji eklemek için sadece yeni class + bu switch genişletilir.
 */
@Component
@RequiredArgsConstructor
public class TransferContext {

    // Her strateji Spring tarafından inject edilir
    private final InternalTransferStrategy internalStrategy;
    private final ExternalTransferStrategy externalStrategy;

    /**
     * IBAN prefix'ine göre uygun stratejiyi seçer ve çalıştırır.
     *
     * @param request  Transfer isteği
     * @return Transfer sonucu
     */
    public TransferResponse executeTransfer(TransferRequest request) {

        // Alıcı IBAN ülke kodunu al (ilk 2 karakter)
        String receiverCountryCode = request.getReceiverIban().substring(0, 2).toUpperCase();

        // TR: Türkiye → banka içi veya aynı ülke farklı banka
        // Demo sistemimizde "TR" = aynı banka içi kabul ediyoruz
        // Gerçekte banka kodu da kontrol edilir (IBAN'ın 5-9. karakterleri)
        TransferStrategy strategy = switch (receiverCountryCode) {
            case "TR" -> internalStrategy;   // Banka içi transfer
            default   -> externalStrategy;   // EFT/SWIFT (yabancı banka)
        };

        return strategy.execute(request);
    }
}
