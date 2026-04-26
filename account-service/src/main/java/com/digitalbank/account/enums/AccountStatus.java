package com.digitalbank.account.enums;

/**
 * Hesap durumlarını tanımlar.
 * Durum geçişleri: ACTIVE ↔ FROZEN, ACTIVE → CLOSED (geri dönüş yok)
 */
public enum AccountStatus {

    /** Aktif hesap: Tüm işlemler mümkün. Normal durum. */
    ACTIVE("Aktif"),

    /**
     * Dondurulmuş hesap: İşlemler engellendi.
     * Sebep: Şüpheli aktivite, müşteri talebi, mahkeme kararı.
     * EMPLOYEE veya ADMIN yetkisi ile dondurulur/çözülür.
     */
    FROZEN("Dondurulmuş"),

    /**
     * Kapalı hesap: Kalıcı olarak devre dışı.
     * Bu durumdan geri dönülemez. Bakiye sıfır olmalıdır.
     */
    CLOSED("Kapalı");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
