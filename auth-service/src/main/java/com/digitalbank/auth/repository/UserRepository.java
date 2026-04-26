package com.digitalbank.auth.repository;

import com.digitalbank.auth.entity.BaseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Kullanıcı veritabanı işlemleri için Repository arayüzü.
 *
 * Spring Data JPA: Metod adından otomatik JPQL üretir.
 * findByEmail → SELECT u FROM BaseUser u WHERE u.email = :email
 * Bu sayede SQL yazmadan veritabanı işlemi yapılabilir.
 */
@Repository
public interface UserRepository extends JpaRepository<BaseUser, UUID> {

    /**
     * Email'e göre kullanıcı bulur — login işleminde kullanılır.
     * Optional: kullanıcı bulunamadığında null yerine Optional.empty() döner,
     * NullPointerException'ı önler.
     */
    Optional<BaseUser> findByEmail(String email);

    /**
     * Email'in sistemde kayıtlı olup olmadığını kontrol eder.
     * COUNT sorgusu döner, tam entity yüklemez → performans için tercih edilir.
     */
    boolean existsByEmail(String email);

    /**
     * TC Kimlik Numarasının sistemde kayıtlı olup olmadığını kontrol eder.
     * JPQL sorgusu: Customer.tcNo alanına erişmek için cast gerekir.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM Customer c WHERE c.tcNo = :tcNo")
    boolean existsByTcNo(String tcNo);
}
