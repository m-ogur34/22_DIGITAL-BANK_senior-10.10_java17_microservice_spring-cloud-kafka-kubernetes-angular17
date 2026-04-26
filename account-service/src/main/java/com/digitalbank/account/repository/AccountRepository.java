package com.digitalbank.account.repository;

import com.digitalbank.account.entity.BaseAccount;
import com.digitalbank.account.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Hesap veritabanı işlemleri.
 * Spring Data JPA polimorfik sorguları destekler:
 * findAll() hem VadesizHesap hem VadeliHesap hem de diğerlerini döner.
 */
@Repository
public interface AccountRepository extends JpaRepository<BaseAccount, UUID> {

    /**
     * IBAN'a göre hesap bulur — transfer işlemlerinde kullanılır.
     */
    Optional<BaseAccount> findByIban(String iban);

    /**
     * IBAN'ın sistemde kayıtlı olup olmadığını kontrol eder.
     */
    boolean existsByIban(String iban);

    /**
     * Müşteriye ait tüm hesapları listeler.
     */
    List<BaseAccount> findByOwnerIdAndStatus(UUID ownerId, AccountStatus status);

    /**
     * Müşteriye ait tüm hesaplar (durum bağımsız).
     */
    List<BaseAccount> findByOwnerId(UUID ownerId);

    /**
     * Müşterinin bakiyesini IBAN ile sorgular — performanslı (sadece bakiye döner).
     */
    @Query("SELECT a.balance FROM BaseAccount a WHERE a.iban = :iban AND a.ownerId = :ownerId")
    Optional<java.math.BigDecimal> findBalanceByIbanAndOwnerId(String iban, UUID ownerId);

    /**
     * Hesap sayısını sayar — müşterinin kaç hesabı olduğunu kontrol etmek için.
     */
    long countByOwnerId(UUID ownerId);
}
