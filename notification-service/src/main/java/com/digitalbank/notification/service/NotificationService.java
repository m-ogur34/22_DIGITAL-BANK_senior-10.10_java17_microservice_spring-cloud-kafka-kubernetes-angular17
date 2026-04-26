package com.digitalbank.notification.service;

import com.digitalbank.notification.entity.Notification;
import com.digitalbank.notification.enums.NotificationType;
import com.digitalbank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bildirim gönderme ve kayıt servisi.
 *
 * Gerçek implementasyon:
 * - SMS: Twilio, Netgsm, İleti Yönetim Sistemi API
 * - Email: Spring Mail (SMTP), SendGrid, AWS SES
 * - Push: Firebase Cloud Messaging (FCM)
 *
 * Simülasyon: Sadece log'a yazar ve veritabanına kaydeder.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * SMS bildirimi gönderir (simülasyon).
     *
     * @param ownerId     Alıcı kullanıcı ID'si
     * @param title       Bildirim başlığı
     * @param message     SMS içeriği
     * @param eventType   Kaynak event türü
     * @param referenceId İlgili kayıt ID'si
     */
    @Transactional
    public void sendSms(UUID ownerId, String title, String message, String eventType, String referenceId) {
        // Gerçek sistemde burada SMS gateway API çağrısı yapılır
        log.info("[SMS SİMÜLASYON] Alıcı: {} | Mesaj: {}", ownerId, message);

        Notification notification = Notification.builder()
                .ownerId(ownerId)
                .title(title)
                .message(message)
                .type(NotificationType.SMS)
                .sent(true) // Simülasyonda her zaman başarılı
                .eventType(eventType)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Email bildirimi gönderir (simülasyon).
     */
    @Transactional
    public void sendEmail(UUID ownerId, String title, String message, String eventType, String referenceId) {
        log.info("[EMAIL SİMÜLASYON] Alıcı: {} | Konu: {} | İçerik: {}", ownerId, title, message);

        Notification notification = Notification.builder()
                .ownerId(ownerId)
                .title(title)
                .message(message)
                .type(NotificationType.EMAIL)
                .sent(true)
                .eventType(eventType)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Acil uyarı (fraud) bildirimi — yönetici ve kullanıcı dahil.
     */
    @Transactional
    public void sendFraudAlert(UUID ownerId, String iban, String reason) {
        String message = String.format(
            "⚠️ DİKKAT: Hesabınızda şüpheli işlem tespit edildi! " +
            "IBAN: %s | Sebep: %s | Güvenliğiniz için hesabınızı kontrol edin.",
            maskIban(iban), reason
        );

        log.warn("[FRAUD ALERT] Kullanıcı: {} | IBAN: {} | Sebep: {}", ownerId, iban, reason);

        // SMS + Email her ikisine de gönder
        sendSms(ownerId, "Şüpheli İşlem Uyarısı", message, "FRAUD_ALERT", null);
        sendEmail(ownerId, "Güvenlik Uyarısı", message, "FRAUD_ALERT", null);
    }

    // IBAN'ın ortasını gizle: TR33...9876
    private String maskIban(String iban) {
        if (iban == null || iban.length() < 10) return "***";
        return iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4);
    }
}
