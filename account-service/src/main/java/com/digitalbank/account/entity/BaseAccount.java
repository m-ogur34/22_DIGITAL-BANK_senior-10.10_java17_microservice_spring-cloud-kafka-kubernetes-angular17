package com.digitalbank.account.entity;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import com.digitalbank.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tüm hesap tiplerinin temel soyut sınıfı.
 *
 * OOP Polimorfizm örneği:
 * faizHesapla() metodu her alt sınıfta farklı implement edilir:
 * - VadesizHesap: faiz yok → BigDecimal.ZERO
 * - VadeliHesap: anapara * faizOrani * gun / 365
 * - YatirimHesap: portföy değerine göre hesap
 *
 * @Inheritance(JOINED): Her alt sınıf kendi tablosuna sahip
 * accounts tablosu + vadesiz_hesaplar tablosu JOIN edilerek tam nesne oluşur.
 */
@Getter
@Setter
@Entity
@Table(name = "accounts", schema = "account_schema",
    indexes = {
        @Index(name = "idx_accounts_iban", columnList = "iban", unique = true),
        @Index(name = "idx_accounts_owner_id", columnList = "owner_id")
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "account_type_disc", discriminatorType = DiscriminatorType.STRING)
public abstract class BaseAccount extends BaseEntity {

    /**
     * IBAN (International Bank Account Number).
     * IbanGenerator.generate() ile TR formatında üretilir.
     * unique = true: Dünyada iki hesabın aynı IBAN'ı olamaz.
     */
    @Column(name = "iban", nullable = false, unique = true, length = 26)
    private String iban;

    /**
     * Hesap bakiyesi.
     * precision = 15: Toplam basamak sayısı (milyarlarca TL)
     * scale = 2: Ondalık basamak (kuruş hassasiyeti)
     *
     * NEDEN BigDecimal?
     * double ile: 1234567.89 + 0.01 = 1234567.90? Hayır! IEEE 754 hatası!
     * BigDecimal ile: Tam aritmetik, finansal hassasiyet garanti.
     *
     * @Version ile optimistic locking:
     * İki farklı işlem aynı anda bakiyeyi güncellemek isterse,
     * birincisi başarılı olur, ikincisi OptimisticLockException alır.
     * Bu sayede "lost update" problemi önlenir.
     * (Örn: Eşzamanlı iki transfer aynı hesaptan yapılamaz)
     */
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Hesap tipi — enum olarak saklanır.
     * @Enumerated(STRING): Veritabanında "VADESIZ" string'i saklanır
     * Alternatif ORDINAL: 0,1,2 sayısı — enum sırası değişirse BOZULUR!
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Hesap durumu: ACTIVE, FROZEN, CLOSED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * Hesap sahibinin kullanıcı ID'si (auth-service'den UUID).
     * FK yok! Servisler arası ilişkide FK yerine uygulama katmanında doğrulama yapılır.
     * Neden? Farklı servisler farklı DB schema'larında — cross-schema FK production'da sorunlu.
     */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Para birimi — şimdilik sadece TRY
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "TRY";

    // Hesap adı/takma ad (müşteri tarafından verilebilir)
    @Column(name = "account_name", length = 100)
    private String accountName;

    /**
     * Hesap için faiz hesaplama — her alt sınıf kendi mantığını uygular.
     * Template Method Pattern: Bu metod abstract, algoritmayı alt sınıflar belirler.
     * @return Hesaplanan faiz tutarı
     */
    public abstract BigDecimal faizHesapla();

    /**
     * Bu hesapta işlem yapılabilir mi?
     * Dondurulmuş ve kapalı hesaplarda işlem yapılamaz.
     * Her alt sınıf ek kısıtlar ekleyebilir (örn: vadeli hesapta vade dolmadan çekim yok).
     * @return İşlem izni varsa true
     */
    public abstract boolean islemIzniVar();
}
