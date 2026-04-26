package com.digitalbank.transaction.entity;

import com.digitalbank.common.entity.BaseEntity;
import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Para transferi kaydı.
 *
 * Her transfer işlemi veritabanında iki kayıt oluşturur (double-entry bookkeeping):
 * 1. Gönderen hesap için DEBIT kaydı (bakiye azalır)
 * 2. Alan hesap için CREDIT kaydı (bakiye artar)
 *
 * Bu yaklaşım:
 * - Muhasebe uyumluluğu sağlar
 * - Audit trail oluşturur (tüm bakiye hareketleri izlenebilir)
 * - Saga rollback'i kolaylaştırır (REVERSAL kaydı eklenir)
 *
 * @Version: Optimistic locking — durum güncellemelerinde çakışma önlenir
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions", schema = "transaction_schema",
    indexes = {
        @Index(name = "idx_txn_sender_iban", columnList = "sender_iban"),
        @Index(name = "idx_txn_receiver_iban", columnList = "receiver_iban"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_owner_id", columnList = "owner_id"),
        @Index(name = "idx_txn_created_at", columnList = "created_at")
    }
)
public class Transaction extends BaseEntity {

    // Gönderen IBAN
    @Column(name = "sender_iban", nullable = false, length = 26)
    private String senderIban;

    // Alıcı IBAN
    @Column(name = "receiver_iban", nullable = false, length = 26)
    private String receiverIban;

    /**
     * Transfer tutarı — BigDecimal: finansal hassasiyet zorunlu.
     * precision=15: Milyar TL'ye kadar işlem desteklenir
     * scale=2: Kuruş hassasiyeti
     */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // İşlem açıklaması (IBAN doğrulama numarası, açıklama notu)
    @Column(name = "description", length = 255)
    private String description;

    // İşlem türü: DEBIT, CREDIT, REVERSAL
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    // İşlem durumu
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    // İşlemi başlatan kullanıcı
    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    // Referans numarası — bağlantılı işlemleri gruplayır (debit ve credit aynı ref'i paylaşır)
    @Column(name = "reference_id", length = 36)
    private String referenceId;

    // Hangi işlemin geri alması (reversal için orijinal işlem ID)
    @Column(name = "reversed_transaction_id", length = 36)
    private String reversedTransactionId;

    // Hata mesajı — FAILED durumunda nedeni saklar
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // Banka içi mi yoksa dışı mı?
    @Column(name = "is_internal", nullable = false)
    private boolean internal = true;
}
