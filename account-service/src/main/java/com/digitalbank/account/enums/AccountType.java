package com.digitalbank.account.enums;

/**
 * Hesap tiplerini tanımlar.
 * Her tip farklı faiz hesaplama ve işlem kurallarına sahiptir.
 */
public enum AccountType {

    /** Vadesiz hesap: Anlık para çekme ve yatırma mümkün. Faiz düşük veya yok. */
    VADESIZ("Vadesiz Hesap"),

    /** Vadeli hesap: Belirli bir süre para çekilemez. Faiz yüksek. */
    VADELI("Vadeli Mevduat Hesabı"),

    /** Tasarruf hesabı: Düzenli birikim için. Orta düzey faiz. */
    TASARRUF("Tasarruf Hesabı"),

    /** Yatırım hesabı: Hisse, fon alım satımı için. Faiz yok, değer artışı hedeflenir. */
    YATIRIM("Yatırım Hesabı");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
