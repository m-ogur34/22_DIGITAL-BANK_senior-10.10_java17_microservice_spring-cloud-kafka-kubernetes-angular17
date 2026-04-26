package com.digitalbank.notification.consumer;

import com.digitalbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Transaction event'lerini Kafka'dan dinleyen consumer.
 *
 * @KafkaListener:
 * - topics: Dinlenen topic adı
 * - groupId: Consumer group — aynı group'taki consumer'lar yükü paylaşır
 *   Farklı group → her consumer tüm mesajları alır (fan-out)
 *
 * @RetryableTopic: Hata durumunda otomatik yeniden deneme.
 * - attempts: Toplam deneme sayısı (1 orijinal + 2 yeniden)
 * - backoff: Her denemede bekleme süresi (exponential backoff)
 * - dltTopicSuffix: Başarısız mesajlar Dead Letter Topic'e gönderilir
 *   DLT: "transaction-events-dlt" → manuel inceleme için
 *
 * Neden DLT? Mesaj işlenemezse kaybolmasın, tekrar işlensin veya incelensin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),  // 1sn, 2sn, 4sn
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlt"
        // Başarısız mesajlar "transaction-events-dlt" topic'ine yazılır
    )
    @KafkaListener(
        topics = "transaction-events",
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionEvent(Map<String, Object> event) {
        try {
            log.info("Transaction event alındı: {}", event);

            String status = (String) event.get("status");
            String ownerIdStr = (String) event.get("ownerId");
            String amount = event.getOrDefault("amount", "?").toString();
            String referenceId = (String) event.get("referenceId");

            if (ownerIdStr == null) {
                log.warn("ownerId boş, bildirim atlandı");
                return;
            }

            UUID ownerId = UUID.fromString(ownerIdStr);

            // Transfer durumuna göre farklı mesaj
            if ("COMPLETED".equals(status)) {
                String message = String.format(
                    "%s TL transfer işleminiz başarıyla tamamlandı. " +
                    "Referans No: %s", amount, referenceId);
                notificationService.sendSms(ownerId,
                    "Transfer Tamamlandı", message, "TRANSACTION_COMPLETED", referenceId);

            } else if ("FAILED".equals(status)) {
                String message = String.format(
                    "%s TL transfer işleminiz başarısız oldu. " +
                    "Referans No: %s", amount, referenceId);
                notificationService.sendSms(ownerId,
                    "Transfer Başarısız", message, "TRANSACTION_FAILED", referenceId);
            }

        } catch (Exception e) {
            // @RetryableTopic yeniden deneyecek
            log.error("Transaction event işlenemedi: {} | Hata: {}", event, e.getMessage());
            throw e; // Exception fırlatılmazsa Kafka yeniden denemez!
        }
    }
}
