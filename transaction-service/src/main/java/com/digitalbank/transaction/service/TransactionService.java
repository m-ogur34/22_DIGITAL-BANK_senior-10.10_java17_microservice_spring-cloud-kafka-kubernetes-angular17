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
 * İşlem servisi — iş mantığı orkestrasyonu.
 * Transfer Saga'yı çağırır, Elasticsearch aramasını yönetir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransferSaga transferSaga;
    private final TransactionRepository transactionRepository;
    private final TransactionSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Para transferi başlatır.
     * Saga pattern ile koordinasyon sağlanır.
     *
     * @param request  Transfer bilgileri
     * @param userId   İşlemi yapan kullanıcı
     */
    public TransferResponse transfer(TransferRequest request, String userId) {
        request.setOwnerId(userId);

        // Saga'yı başlat — başarısız olursa exception fırlatır
        TransferResponse response = transferSaga.execute(request);

        // Elasticsearch'e kaydet (arama için)
        saveToElasticsearch(request, response);

        return response;
    }

    /**
     * İşlem geçmişini sayfalı olarak döner (PostgreSQL'den).
     */
    public Page<Transaction> getTransactionHistory(String userId, int page, int size) {
        return transactionRepository.findByOwnerIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    /**
     * Elasticsearch üzerinden işlem arama.
     * Keyword, tarih aralığı, tutar aralığı, IBAN filtreleme desteklenir.
     *
     * CriteriaQuery: Koşulları programatik olarak oluşturur (QueryDSL'e benzer).
     * Alternatif: @Query anotasyonu ile native ES sorgusu
     */
    public List<TransactionDocument> searchTransactions(TransactionSearchRequest searchRequest, String userId) {

        // Temel kriter: sadece kendi işlemlerini görsün
        Criteria criteria = new Criteria("owner_id").is(userId);

        // Keyword arama (açıklama full-text)
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().isBlank()) {
            criteria = criteria.and(new Criteria("description").matches(searchRequest.getKeyword()));
        }

        // IBAN filtresi
        if (searchRequest.getIban() != null && !searchRequest.getIban().isBlank()) {
            Criteria ibanCriteria = new Criteria("sender_iban").is(searchRequest.getIban())
                    .or(new Criteria("receiver_iban").is(searchRequest.getIban()));
            criteria = criteria.and(ibanCriteria);
        }

        // Tarih aralığı filtresi
        if (searchRequest.getStartDate() != null && searchRequest.getEndDate() != null) {
            criteria = criteria.and(
                new Criteria("created_at").between(
                    searchRequest.getStartDate(),
                    searchRequest.getEndDate()
                )
            );
        }

        // Tutar aralığı filtresi
        if (searchRequest.getMinAmount() != null && searchRequest.getMaxAmount() != null) {
            criteria = criteria.and(
                new Criteria("amount").between(
                    searchRequest.getMinAmount(),
                    searchRequest.getMaxAmount()
                )
            );
        }

        // Durum filtresi
        if (searchRequest.getStatus() != null) {
            criteria = criteria.and(new Criteria("status").is(searchRequest.getStatus().name()));
        }

        CriteriaQuery query = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of(
                    searchRequest.getPage(),
                    searchRequest.getSize(),
                    Sort.by("created_at").descending()
                ));

        SearchHits<TransactionDocument> hits = elasticsearchOperations.search(query, TransactionDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Transfer sonucunu Elasticsearch'e kaydeder.
     * Başarısız olsa bile transfer etkilenmez (eventual consistency).
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
                    .internal(request.getReceiverIban().startsWith("TR"))
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            searchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Elasticsearch'e yazılamadı (transfer başarılı): {}", e.getMessage());
        }
    }

    /**
     * Transfer geri alma (admin veya belirli koşullarda).
     */
    public void reverseTransaction(String referenceId) {
        transferSaga.compensate(referenceId);
    }
}
