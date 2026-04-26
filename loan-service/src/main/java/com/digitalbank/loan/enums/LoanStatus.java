package com.digitalbank.loan.enums;

/**
 * Kredi başvuru durum geçişleri:
 * PENDING → APPROVED → DISBURSED
 * PENDING → REJECTED
 */
public enum LoanStatus {
    /** Başvuru alındı, değerlendiriliyor */
    PENDING("Değerlendiriliyor"),
    /** Kredi onaylandı, disbursement bekliyor */
    APPROVED("Onaylandı"),
    /** Kredi reddedildi */
    REJECTED("Reddedildi"),
    /** Para hesaba aktarıldı, geri ödeme başladı */
    DISBURSED("Kullandırıldı"),
    /** Tüm taksitler ödendi */
    CLOSED("Kapatıldı");

    private final String displayName;
    LoanStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
