package com.digitalbank.notification.enums;

/**
 * Bildirim kanalı türleri.
 * Gerçek sistemde SMS gateway (İleti Yönetim Sistemi) ve email SMTP kullanılır.
 * Bu projede simülasyon — log'a yazılır.
 */
public enum NotificationType {
    /** SMS bildirimi — telefon numarasına gönderilir */
    SMS("SMS"),
    /** Email bildirimi — email adresine gönderilir */
    EMAIL("E-Posta"),
    /** Push notification — mobil uygulama bildirimi */
    PUSH("Mobil Bildirim"),
    /** Sistem içi bildirim */
    IN_APP("Uygulama İçi");

    private final String displayName;
    NotificationType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
