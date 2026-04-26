package com.digitalbank.transaction.service.strategy;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import com.digitalbank.transaction.repository.TransactionRepository;
import com.digitalbank.common.exception.InsufficientFundsException;
import com.digitalbank.common.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Banka dışı EFT/SWIFT transfer stratejisi (simülasyon).
 *
 * Gerçek sistemde: SWIFT/BIC koduna göre hedef bankaya mesaj gönderilir.
 * Bu simülasyonda: Dışarıya gidiyormuş gibi işlemi kaydedip başarılı döneriz.
 *
 * Banka dışı transferde:
 * - Daha düşük limit (günlük 50.000 TL)
 * - Daha yüksek komisyon
 * - İşlem 1-2 iş günü sürebilir (T+1)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalTransferStrategy implements TransferStrategy {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public TransferResponse execute(TransferRequest request) {

        String referenceId = UUID.randomUUID().toString();
        log.info("Banka dışı EFT başlatıldı: {} -> {} ({})",
                request.getSenderIban(), request.getReceiverIban(), referenceId);

        // Bakiye kontrolü
        if (!MoneyUtils.isSufficient(request.getCurrentBalance(), request.getAmount())) {
            throw new InsufficientFundsException(request.getSenderIban());
        }

        // Dışarıya çıkış kaydı (EXTERNAL_TRANSFER tipi)
        Transaction externalTxn = Transaction.builder()
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .amount(request.getAmount())
                .description("[EFT] " + request.getDescription())
                .type(TransactionType.EXTERNAL_TRANSFER)
                .status(TransactionStatus.COMPLETED) // Simülasyon: anında başarılı
                .ownerId(request.getOwnerId())
                .referenceId(referenceId)
                .internal(false) // Banka dışı
                .build();

        Transaction saved = transactionRepository.save(externalTxn);
        log.info("EFT tamamlandı: referenceId={}", referenceId);

        return TransferResponse.builder()
                .transactionId(saved.getId().toString())
                .referenceId(referenceId)
                .status(TransactionStatus.COMPLETED)
                .message("EFT başarıyla gönderildi")
                .amount(request.getAmount())
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .build();
    }
}
