package com.digitalbank.notification.consumer;

import com.digitalbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Fraud (dolandırıcılık) alert'lerini dinleyen consumer.
 * Kritik güvenlik mesajları — SMS + Email her ikisine gönderilir.
 * @RetryableTopic kullanmıyoruz: Fraud alert'i anında işlenmeli.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAlertConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "fraud-alert-events", groupId = "notification-group")
    public void handleFraudAlert(Map<String, Object> event) {
        log.warn("⚠️ FRAUD ALERT alındı: {}", event);

        String userIdStr = (String) event.get("userId");
        String iban = (String) event.get("iban");
        String reason = (String) event.get("reason");

        if (userIdStr == null || iban == null) {
            log.error("Fraud alert eksik bilgi: {}", event);
            return;
        }

        try {
            UUID ownerId = UUID.fromString(userIdStr);
            notificationService.sendFraudAlert(ownerId, iban, reason);
        } catch (Exception e) {
            log.error("Fraud alert bildirimi gönderilemedi: {}", e.getMessage());
        }
    }
}
