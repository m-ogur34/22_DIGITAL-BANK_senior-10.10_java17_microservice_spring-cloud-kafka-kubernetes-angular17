package com.digitalbank.transaction.enums;

/**
 * İşlem durumu geçişleri:
 * PENDING → PROCESSING → COMPLETED
 * PENDING → PROCESSING → FAILED
 * COMPLETED → REVERSED (Saga compensating transaction)
 *
 * Bu durum makinesi Saga pattern ile yönetilir.
 */
public enum TransactionStatus {

    /** İşlem kuyruğa alındı, henüz işlenmedi */
    PENDING("Beklemede"),

    /** İşlem işleniyor — debit/credit atomik olarak yapılıyor */
    PROCESSING("İşleniyor"),

    /** İşlem başarıyla tamamlandı */
    COMPLETED("Tamamlandı"),

    /** İşlem başarısız oldu — yetersiz bakiye, limit aşımı vb. */
    FAILED("Başarısız"),

    /** Tamamlanan işlem geri alındı — Saga compensating transaction */
    REVERSED("İptal Edildi");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
