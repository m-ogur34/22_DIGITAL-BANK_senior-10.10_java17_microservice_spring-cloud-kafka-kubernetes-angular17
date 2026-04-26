package com.digitalbank.notification.consumer;

import com.digitalbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kredi event'lerini dinleyen consumer.
 * loan-events topic'inden APPROVED ve REJECTED mesajlarını işler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventConsumer {

    private final NotificationService notificationService;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = "-dlt")
    @KafkaListener(topics = "loan-events", groupId = "notification-group")
    public void handleLoanEvent(Map<String, Object> event) {
        try {
            log.info("Loan event alındı: {}", event);

            String status = (String) event.get("status");
            String ownerIdStr = (String) event.get("ownerId");
            String loanId = (String) event.get("loanId");

            if (ownerIdStr == null) return;
            UUID ownerId = UUID.fromString(ownerIdStr);

            if ("APPROVED".equals(status)) {
                String amount = event.getOrDefault("approvedAmount", "?").toString();
                String installment = event.getOrDefault("monthlyInstallment", "?").toString();
                String loanTypeName = event.getOrDefault("loanTypeName", "Krediniz").toString();

                String message = String.format(
                    "Tebrikler! %s başvurunuz onaylandı. " +
                    "Onaylanan tutar: %s TL, Aylık taksit: %s TL.",
                    loanTypeName, amount, installment);

                notificationService.sendSms(ownerId, "Krediniz Onaylandı", message, "LOAN_APPROVED", loanId);
                notificationService.sendEmail(ownerId, "Kredi Başvurusu Sonucu", message, "LOAN_APPROVED", loanId);

            } else if ("REJECTED".equals(status)) {
                String reason = (String) event.getOrDefault("rejectionReason", "Koşullar sağlanamadı");
                String message = String.format("Kredi başvurunuz değerlendirme sonucu reddedildi. Sebep: %s", reason);
                notificationService.sendEmail(ownerId, "Kredi Başvurusu Reddedildi", message, "LOAN_REJECTED", loanId);
            }

        } catch (Exception e) {
            log.error("Loan event işlenemedi: {} | Hata: {}", event, e.getMessage());
            throw e;
        }
    }
}
