package com.digitalbank.transaction.document;

import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elasticsearch'te saklanan işlem belgesi.
 *
 * PostgreSQL: ACID işlemler, finansal doğruluk
 * Elasticsearch: Full-text arama, tarih/tutar filtreleme, sayfalama
 *
 * Her başarılı işlem hem PostgreSQL'e hem de ES'e yazılır.
 * ES'e yazma asenkron (Kafka consumer'dan) veya senkron yapılabilir.
 * Burada senkron yaklaşım kullanıyoruz.
 *
 * @Document: ES index adı — her servis kendi index'ini kullanır.
 * index = "transactions": tüm işlemler tek index'te
 */
@Data
@Builder
@Document(indexName = "transactions")
public class TransactionDocument {

    @Id
    private String id;

    // IBAN'lara göre filtreleme için keyword (analiz edilmez)
    @Field(type = FieldType.Keyword, name = "sender_iban")
    private String senderIban;

    @Field(type = FieldType.Keyword, name = "receiver_iban")
    private String receiverIban;

    // Tutar aralığına göre filtreleme için double
    @Field(type = FieldType.Double, name = "amount")
    private BigDecimal amount;

    /**
     * Açıklama alanı full-text arama için text tipinde.
     * Text: Analiz edilir, tokenize edilir — "havale ödemesi" → ["havale", "ödemesi"]
     * Keyword: Tam eşleşme için — status, iban gibi alanlar
     */
    @Field(type = FieldType.Text, name = "description", analyzer = "turkish")
    private String description;

    @Field(type = FieldType.Keyword, name = "type")
    private TransactionType type;

    @Field(type = FieldType.Keyword, name = "status")
    private TransactionStatus status;

    @Field(type = FieldType.Keyword, name = "owner_id")
    private String ownerId;

    @Field(type = FieldType.Keyword, name = "reference_id")
    private String referenceId;

    @Field(type = FieldType.Boolean, name = "is_internal")
    private boolean internal;

    // Tarih filtreleme için date tipi
    @Field(type = FieldType.Date, name = "created_at", format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
