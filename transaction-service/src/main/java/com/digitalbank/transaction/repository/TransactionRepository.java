package com.digitalbank.transaction.repository;

import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * İşlem veritabanı sorguları.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Referans ID'ye göre tüm ilişkili işlemleri bulur.
     * Debit + Credit kaydı aynı referenceId'yi paylaşır.
     */
    List<Transaction> findByReferenceId(String referenceId);

    /**
     * Kullanıcının tüm işlemlerini tarih sıralamasıyla döner (sayfalı).
     * Pageable: Spring Data pagination — page, size, sort bilgisi taşır.
     */
    Page<Transaction> findByOwnerIdOrderByCreatedAtDesc(String ownerId, Pageable pageable);

    /**
     * IBAN'a göre işlemleri bulur (gönderen veya alıcı).
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderIban = :iban OR t.receiverIban = :iban ORDER BY t.createdAt DESC")
    Page<Transaction> findByIban(String iban, Pageable pageable);
}
