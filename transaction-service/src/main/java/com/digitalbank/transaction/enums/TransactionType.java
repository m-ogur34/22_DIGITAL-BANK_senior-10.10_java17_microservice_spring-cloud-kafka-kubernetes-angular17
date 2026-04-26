package com.digitalbank.transaction.enums;

/**
 * İşlem tipi — muhasebe açısından her transferde iki kayıt olur:
 * Gönderen hesap: DEBIT (borç — bakiye azalır)
 * Alan hesap: CREDIT (alacak — bakiye artar)
 * Geri alma: REVERSAL
 */
public enum TransactionType {

    /** Para çıkışı — gönderen hesabın bakiyesi azalır */
    DEBIT("Para Çıkışı"),

    /** Para girişi — alan hesabın bakiyesi artar */
    CREDIT("Para Girişi"),

    /** İşlem iptali — REVERSAL */
    REVERSAL("İptal"),

    /** Banka dışı transfer (simülasyon) */
    EXTERNAL_TRANSFER("Dış Transfer"),

    /** Kredi ödemesi */
    LOAN_PAYMENT("Kredi Ödemesi");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
