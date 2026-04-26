package com.digitalbank.notification.entity;

import com.digitalbank.common.entity.BaseEntity;
import com.digitalbank.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Gönderilen bildirimlerin kaydı.
 * Her Kafka mesajı işlendiğinde buraya kayıt düşülür.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications", schema = "notification_schema",
    indexes = {
        @Index(name = "idx_notif_owner_id", columnList = "owner_id"),
        @Index(name = "idx_notif_created_at", columnList = "created_at")
    }
)
public class Notification extends BaseEntity {

    // Bildirim alan kullanıcı
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Bildirim başlığı
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // Bildirim içeriği
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // Bildirim kanalı
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    // Gönderildi mi?
    @Column(name = "is_sent", nullable = false)
    private boolean sent = false;

    // Hata mesajı (gönderilemezse)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // Kaynak event (transaction-completed, loan-approved vb.)
    @Column(name = "event_type", length = 50)
    private String eventType;

    // İlgili kayıt ID (işlem ID, kredi ID vb.)
    @Column(name = "reference_id", length = 36)
    private String referenceId;
}
