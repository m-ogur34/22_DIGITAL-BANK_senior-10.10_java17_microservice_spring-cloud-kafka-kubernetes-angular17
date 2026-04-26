package com.digitalbank.transaction.repository;

import com.digitalbank.transaction.document.TransactionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch işlem arama repository'si.
 *
 * ElasticsearchRepository: Spring Data'nın Elasticsearch sürümü.
 * Basit sorgular metod adından otomatik üretilir — JPA Repository'ye benzer.
 * Karmaşık sorgular için ElasticsearchOperations kullanılır.
 */
@Repository
public interface TransactionSearchRepository extends ElasticsearchRepository<TransactionDocument, String> {

    /**
     * Kullanıcıya ait son işlemleri listeler.
     */
    List<TransactionDocument> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    /**
     * IBAN'a göre işlem arama (gönderen veya alıcı).
     */
    List<TransactionDocument> findBySenderIbanOrReceiverIban(String senderIban, String receiverIban);
}
