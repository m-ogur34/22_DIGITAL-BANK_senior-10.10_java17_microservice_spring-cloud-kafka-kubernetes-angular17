package com.digitalbank.transaction.saga;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import com.digitalbank.transaction.kafka.TransactionEventProducer;
import com.digitalbank.transaction.repository.TransactionRepository;
import com.digitalbank.transaction.service.strategy.TransferContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transfer işleminin Saga koordinatörü.
 *
 * Saga Pattern:
 * Distributed transaction problemini çözmek için kullanılır.
 * Microservice mimarisinde tek bir 2-phase commit (2PC) yerine
 * birbirine bağlı lokal transaction'lar zinciri kullanılır.
 *
 * Bu sistemdeki Saga adımları:
 * 1. Transfer isteğini doğrula
 * 2. InternalTransferStrategy.execute() → DEBIT + CREDIT kayıtları
 * 3. Elasticsearch'e kaydet (arama için)
 * 4. Kafka'ya event yayınla (bildirim için)
 *
 * Başarısız adımda compensating transaction:
 * - 2. adım başarısızsa: FAILED kaydı yaz
 * - 3. adım başarısızsa: Elasticsearch'e yazamadık ama DB'de kayıt var (eventual consistency)
 * - 4. adım başarısızsa: Kafka'ya yazamadık ama transfer gerçekleşti (bildirim gecikmeli gidebilir)
 *
 * Rollback (REVERSAL) işlemi:
 * Tamamlanmış bir transferi geri almak için DEBIT→CREDIT, CREDIT→DEBIT çevrilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferSaga {

    private final TransferContext transferContext;
    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    /**
     * Transfer saga'sını başlatır.
     * Her adım loglama ile takip edilir.
     *
     * @param request  Transfer isteği
     * @return Transfer sonucu
     */
    public TransferResponse execute(TransferRequest request) {
        log.info("Transfer Saga başlatıldı: {} → {} ({})",
                request.getSenderIban(), request.getReceiverIban(), request.getAmount());

        TransferResponse response;

        try {
            // Adım 1: Transfer stratejisini seç ve çalıştır
            response = transferContext.executeTransfer(request);
            log.info("Saga Adım 1 ✓ — Transfer tamamlandı: ref={}", response.getReferenceId());

        } catch (Exception e) {
            // Transfer başarısız — compensating action zaten strateji içinde yapıldı (FAILED kaydı)
            log.error("Saga Adım 1 ✗ — Transfer başarısız: {}", e.getMessage());

            // Hata event'ini Kafka'ya yayınla
            TransferResponse failedResponse = TransferResponse.builder()
                    .status(TransactionStatus.FAILED)
                    .message(e.getMessage())
                    .senderIban(request.getSenderIban())
                    .receiverIban(request.getReceiverIban())
                    .amount(request.getAmount())
                    .build();
            eventProducer.publishTransactionFailed(failedResponse);

            throw e; // Exception'ı yukarı taşı
        }

        // Adım 2: Kafka'ya event yayınla (asenkron — başarısız olursa transfer etkilenmez)
        try {
            eventProducer.publishTransactionCompleted(response);
            log.info("Saga Adım 2 ✓ — Kafka event yayınlandı");
        } catch (Exception e) {
            // Kafka yazma hatası transfer'i etkilemez — eventual consistency
            log.warn("Saga Adım 2 ✗ — Kafka event yayınlanamadı (transfer başarılı): {}", e.getMessage());
        }

        return response;
    }

    /**
     * Tamamlanmış bir transferi geri alır (Compensating Transaction).
     *
     * Bu işlem:
     * - Orijinal DEBIT için CREDIT kaydı ekler (para geri gelir)
     * - Orijinal CREDIT için DEBIT kaydı ekler (para geri alınır)
     * - Orijinal kayıtların status'unu REVERSED yapar
     *
     * @param referenceId  Geri alınacak transfer'in referans ID'si
     */
    public void compensate(String referenceId) {
        log.info("Compensating transaction başlatıldı: referenceId={}", referenceId);

        List<Transaction> originalTransactions = transactionRepository.findByReferenceId(referenceId);

        if (originalTransactions.isEmpty()) {
            throw new IllegalArgumentException("Transfer bulunamadı: " + referenceId);
        }

        for (Transaction original : originalTransactions) {
            // Orijinal kaydı REVERSED olarak işaretle
            original.setStatus(TransactionStatus.REVERSED);
            transactionRepository.save(original);

            // Compensating transaction: DEBIT ↔ CREDIT tersine çevir
            TransactionType compensatingType = original.getType() == TransactionType.DEBIT
                    ? TransactionType.CREDIT
                    : TransactionType.DEBIT;

            Transaction compensating = Transaction.builder()
                    .senderIban(original.getReceiverIban()) // Kaynak ve hedef yer değiştirir
                    .receiverIban(original.getSenderIban())
                    .amount(original.getAmount())
                    .description("[İPTAL] " + original.getDescription())
                    .type(compensatingType)
                    .status(TransactionStatus.COMPLETED)
                    .ownerId(original.getOwnerId())
                    .referenceId("REVERSAL-" + referenceId)
                    .reversedTransactionId(original.getId().toString())
                    .internal(original.isInternal())
                    .build();

            transactionRepository.save(compensating);
        }

        log.info("Compensating transaction tamamlandı: referenceId={}", referenceId);
    }
}
