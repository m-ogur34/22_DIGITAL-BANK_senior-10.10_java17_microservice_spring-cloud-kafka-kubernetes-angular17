package com.digitalbank.transaction.kafka;

import com.digitalbank.transaction.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * İşlem event'lerini Kafka'ya yayınlar.
 *
 * Kafka Topic'leri:
 * - transaction-events: Tüm işlem olayları (completed, failed)
 *   → notification-service dinler → SMS/email bildirimi gönderir
 *
 * Mesaj formatı (JSON):
 * {
 *   "transactionId": "uuid",
 *   "referenceId": "uuid",
 *   "status": "COMPLETED",
 *   "amount": "1500.00",
 *   "senderIban": "TR33...",
 *   "receiverIban": "TR44...",
 *   "message": "Transfer başarıyla tamamlandı"
 * }
 *
 * Neden Kafka?
 * - Notification service devre dışı olsa bile mesajlar kaybolmaz (durable log)
 * - Transaction service notification'ı beklemek zorunda değil (async)
 * - Birden fazla consumer aynı mesajı okuyabilir (fan-out)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic adları — notification-service'in dinlediği topic'ler
    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    private static final String FRAUD_ALERT_TOPIC = "fraud-alert-events";

    /**
     * Transfer tamamlandı event'ini yayınlar.
     * Mesaj key: referenceId — aynı partition'a yazılmasını sağlar (sıralı işlem)
     *
     * @param response  Transfer sonucu
     */
    public void publishTransactionCompleted(TransferResponse response) {
        // CompletableFuture: Mesaj gönderimi asenkron — başarısız olursa loglanır
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, response.getReferenceId(), response);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction event Kafka'ya gönderildi: topic={}, partition={}, offset={}",
                        TRANSACTION_EVENTS_TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Kafka'ya yazılamazsa logla — notification gönderilmeyebilir
                // Prodüksiyon'da: retry mekanizması, dead letter queue
                log.error("Transaction event Kafka'ya gönderilemedi: {}", ex.getMessage());
            }
        });
    }

    /**
     * Transfer başarısız event'i yayınlar.
     */
    public void publishTransactionFailed(TransferResponse response) {
        kafkaTemplate.send(TRANSACTION_EVENTS_TOPIC, response.getReferenceId(), response)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed transaction event gönderilemedi: {}", ex.getMessage());
                    }
                });
    }

    /**
     * Şüpheli işlem uyarısı (fraud detection simülasyonu).
     * Kritik güvenlik event'i — notification-service yöneticiye bildirir.
     *
     * Fraud kriterleri (simülasyon):
     * - Kısa sürede çok sayıda transfer
     * - Yüksek tutarlı transfer
     * - Yeni hesaba büyük transfer
     */
    public void publishFraudAlert(String userId, String iban, String reason) {
        var alertPayload = new java.util.HashMap<String, String>();
        alertPayload.put("userId", userId);
        alertPayload.put("iban", iban);
        alertPayload.put("reason", reason);
        alertPayload.put("timestamp", java.time.LocalDateTime.now().toString());

        kafkaTemplate.send(FRAUD_ALERT_TOPIC, userId, alertPayload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.warn("Fraud alert gönderildi: userId={}, iban={}", userId, iban);
                    } else {
                        log.error("Fraud alert gönderilemedi: {}", ex.getMessage());
                    }
                });
    }
}
