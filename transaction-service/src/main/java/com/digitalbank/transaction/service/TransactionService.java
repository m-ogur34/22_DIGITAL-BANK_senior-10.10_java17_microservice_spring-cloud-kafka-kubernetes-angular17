package com.digitalbank.transaction.service;

import com.digitalbank.transaction.document.TransactionDocument;
import com.digitalbank.transaction.dto.TransactionSearchRequest;
import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.repository.TransactionRepository;
import com.digitalbank.transaction.repository.TransactionSearchRepository;
import com.digitalbank.transaction.saga.TransferSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PARA TRANSFERI SERVİSİ — İş Mantığı Orkestrasyonu
 * ======================================================
 *
 * İki temel sorumluluk:
 *   1. Transfer koordinasyonu → TransferSaga ile
 *   2. İşlem arama → Elasticsearch ile
 *
 * Neden iki ayrı veritabanı (PostgreSQL + Elasticsearch)?
 *   PostgreSQL: Gerçek para hareketi verisi — ACID garantisi şart.
 *               Bakiye, transaction kaydı, referans ID burada.
 *   Elasticsearch: Arama ve raporlama — tam metin arama, filtreler, aggregation.
 *               Müşteri "geçen ay IBAN'a gönderdiğim" diyerek arama yapar.
 *               SQL LIKE ile büyük tabloda tam metin arama yavaştır, ES hızlıdır.
 *
 * Eventual Consistency (Nihai Tutarlılık):
 *   Transfer başarılı → PostgreSQL'e yazılır.
 *   Elasticsearch'e yazma başarısız olursa: Transfer iptal EDİLMEZ.
 *   ES geçici olarak eksik kalır, ama para hareketi gerçekleşmiştir.
 *   Neden? Para operasyonları atomik olmalı. ES hatası para transferini engellememeli.
 *   Çözüm: ES yazma başarısız loglanır, ayrı bir job tekrar dener (retry mechanism).
 *
 * Saga Pattern nedir?
 *   Dağıtık sistemlerde birden fazla servisi kapsayan işlem yönetimi.
 *   Örnek: Transfer = bakiye düşürme (account-service) + kayıt (transaction-service)
 *   Birisi başarısız olursa compensating transaction (telafi) çalışır.
 *   ACID transaction değil — servisler arası koordinasyon.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    // Saga orkestratörü — transfer adımlarını yönetir
    private final TransferSaga transferSaga;

    // PostgreSQL: Kalıcı transaction kayıtları (sayfalama, geçmiş)
    private final TransactionRepository transactionRepository;

    // Elasticsearch repository: basit CRUD (Spring Data ES)
    private final TransactionSearchRepository searchRepository;

    // Elasticsearch: Karmaşık sorgular (CriteriaQuery, aggregation)
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Para transferi başlatır.
     *
     * Akış:
     *   1. request'e ownerId set et (güvenlik: controller'dan gelen userId)
     *   2. Saga'yı başlat:
     *      a. account-service: sender bakiyesini kontrol et
     *      b. account-service: sender'dan düş, receiver'a ekle (atomik)
     *      c. transaction-service: DB'ye kaydet
     *   3. Elasticsearch'e yaz (eventual consistency — hata olursa loglama yeter)
     *
     * Neden ownerId controller'dan alınır?
     *   Güvenlik: Kullanıcı kendi IBAN'ından başka birinin adına transfer yapamaz.
     *   Controller'da JWT'den extract edilen userId burada set edilir.
     *   Request body'de gelen userId'ye güvenilmez — manipüle edilebilir.
     *
     * @param request Transfer bilgileri (IBAN'lar, tutar, açıklama)
     * @param userId  JWT'den alınan kimlik doğrulanmış kullanıcı ID'si
     */
    public TransferResponse transfer(TransferRequest request, String userId) {
        request.setOwnerId(userId);

        // Saga: Başarısız adımlarda compensating transaction çalışır
        // Hata: IllegalStateException (yetersiz bakiye), ExternalServiceException vb.
        TransferResponse response = transferSaga.execute(request);

        // Elasticsearch'e kaydet — eventual consistency
        // Bu başarısız olursa transfer geri alınmaz
        saveToElasticsearch(request, response);

        return response;
    }

    /**
     * Kullanıcının işlem geçmişini sayfalı getirir (PostgreSQL'den).
     *
     * Neden Elasticsearch değil PostgreSQL?
     *   İşlem geçmişi liste: pagination yeterli, tam metin arama gerekmez.
     *   PostgreSQL: tutarlı, sorted, indeksli (owner_id + created_at).
     *   Elasticsearch: arama gerektirmeyen sıralı listeleme için gereksiz overhead.
     *   Kural: Arama → ES, Liste/pagination → SQL.
     *
     * @param userId Kullanıcı ID'si (kendi işlemlerini görür)
     * @param page   Sayfa numarası (0'dan başlar)
     * @param size   Sayfa boyutu
     */
    public Page<Transaction> getTransactionHistory(String userId, int page, int size) {
        return transactionRepository.findByOwnerIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    /**
     * Elasticsearch üzerinden işlem araması.
     *
     * CriteriaQuery — Programatik sorgu oluşturma:
     *   SQL'deki WHERE koşulları gibi düşün ama ES'e özgü.
     *   Her criteria.and() → bir AND koşulu ekler.
     *   Criteria("field").is(value) → term query (exact match)
     *   Criteria("field").matches(text) → match query (full-text, tokenize eder)
     *   Criteria("field").between(min, max) → range query
     *
     * Alternatif yaklaşımlar:
     *   @Query annotasyonu: native JSON sorgu — daha güçlü ama string içinde yazılır, tip güvenliği yok
     *   QueryBuilder DSL: daha verbose ama esnek
     *   CriteriaQuery: Java tipi güvenli, okunabilir — bu projede seçilen
     *
     * Neden owner_id her zaman zorunlu?
     *   Güvenlik: Kullanıcı yalnızca kendi işlemlerini görebilir.
     *   Temel kriter silinirse tüm kullanıcıların işlemleri görünür — güvenlik açığı.
     *
     * @param searchRequest Arama kriterleri (keyword, tarih, tutar, IBAN, durum)
     * @param userId        JWT'den alınan kullanıcı ID'si (güvenlik filtresi)
     */
    public List<TransactionDocument> searchTransactions(TransactionSearchRequest searchRequest, String userId) {

        // Temel güvenlik filtresi: sadece kendi işlemlerini gör
        Criteria criteria = new Criteria("owner_id").is(userId);

        // Açıklama arama — match query: tokenize eder, "ödeme" arar → "ödeme transferi" bulur
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isBlank()) {
            criteria = criteria.and(new Criteria("description").matches(searchRequest.getKeyword()));
        }

        // IBAN filtresi — gönderen VEYA alıcı IBAN ile ara (OR koşulu)
        if (searchRequest.getIban() != null && !searchRequest.getIban().isBlank()) {
            Criteria ibanCriteria = new Criteria("sender_iban").is(searchRequest.getIban())
                    .or(new Criteria("receiver_iban").is(searchRequest.getIban()));
            criteria = criteria.and(ibanCriteria);
        }

        // Tarih aralığı — range query: başlangıç ve bitiş tarihleri arası
        if (searchRequest.getStartDate() != null && searchRequest.getEndDate() != null) {
            criteria = criteria.and(
                new Criteria("created_at").between(
                    searchRequest.getStartDate(),
                    searchRequest.getEndDate()
                )
            );
        }

        // Tutar aralığı — kaç TL ile kaç TL arası işlemler
        if (searchRequest.getMinAmount() != null && searchRequest.getMaxAmount() != null) {
            criteria = criteria.and(
                new Criteria("amount").between(
                    searchRequest.getMinAmount(),
                    searchRequest.getMaxAmount()
                )
            );
        }

        // Durum filtresi — PENDING, COMPLETED, FAILED
        if (searchRequest.getStatus() != null) {
            criteria = criteria.and(new Criteria("status").is(searchRequest.getStatus().name()));
        }

        // Sorguyu oluştur: criteria + sayfalama + sıralama
        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(
                    searchRequest.getPage(),
                    searchRequest.getSize(),
                    Sort.by("created_at").descending()  // En yeni önce
                ));

        // Elasticsearch'te ara — SearchHit<T> ile sonuçları al
        SearchHits<TransactionDocument> hits = elasticsearchOperations.search(query, TransactionDocument.class);

        // SearchHit wrapper'ından içeriği çıkar
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Transfer sonucunu Elasticsearch'e kaydeder.
     *
     * try-catch: ES hatası transfer'i geri almaz.
     *   Para hareketi gerçekleşti — ES sadece arama için.
     *   Hata loglanır, arka planda retry job çalışır.
     *
     * internal alanı: Yurt içi transfer mi?
     *   TR ile başlayan IBAN → Türk bankası → internal=true.
     *   Yabancı IBAN → SWIFT transfer → internal=false.
     */
    private void saveToElasticsearch(TransferRequest request, TransferResponse response) {
        try {
            TransactionDocument doc = TransactionDocument.builder()
                    .id(response.getTransactionId())
                    .senderIban(request.getSenderIban())
                    .receiverIban(request.getReceiverIban())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .status(response.getStatus())
                    .ownerId(request.getOwnerId())
                    .referenceId(response.getReferenceId())
                    // TR ile başlayan IBAN → yurt içi transfer
                    .internal(request.getReceiverIban().startsWith("TR"))
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            searchRepository.save(doc);
        } catch (Exception e) {
            // ES hatası tranfer'i iptal etmez — eventual consistency
            log.warn("Elasticsearch'e yazılamadı (transfer başarılı oldu, arama gecikmeli): {}", e.getMessage());
        }
    }

    /**
     * Transfer geri alma — yalnızca belirli koşullarda (admin veya otomatik).
     *
     * compensate(): Saga'nın telafi adımını çalıştırır.
     *   Gönderilen para iade edilir — account-service'te bakiye güncellenir.
     *   Tüm adımlar geri alınamıyorsa (örn. alıcı parayı çekti) manuel süreç başlar.
     */
    public void reverseTransaction(String referenceId) {
        transferSaga.compensate(referenceId);
    }
}
