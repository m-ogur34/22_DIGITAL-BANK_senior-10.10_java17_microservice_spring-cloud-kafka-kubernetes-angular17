package com.digitalbank.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tüm entity'lerin kalıtım aldığı temel sınıf.
 *
 * @MappedSuperclass: Bu sınıfın kendisi bir tablo oluşturmaz,
 * ama alanları alt sınıfların tablolarına dahil edilir.
 * Alternatif: @Inheritance(strategy=TABLE_PER_CLASS) — her alt sınıf kendi tam tablosunu alır.
 * Burada @MappedSuperclass tercih edilir çünkü daha esnek ve overhead'i düşüktür.
 *
 * @EntityListeners: JPA'nın @CreatedDate ve @LastModifiedDate anotasyonlarını
 * otomatik doldurmak için AuditingEntityListener'ı aktif ederiz.
 * Servislerde @EnableJpaAuditing anotasyonu da gereklidir.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Birincil anahtar olarak UUID kullanıyoruz.
     * Neden UUID? Dağıtık sistemlerde farklı servisler aynı ID'yi üretemez,
     * böylece ID çakışması olmaz. Dezavantajı: BIGINT'e göre daha fazla depolama alanı.
     *
     * @GeneratedValue(strategy = GenerationType.UUID): JPA 3.1 ile gelen özellik,
     * veritabanından bağımsız UUID üretimi sağlar.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Kayıt oluşturulma zamanı.
     * @CreatedDate: AuditingEntityListener tarafından persist sırasında otomatik doldurulur.
     * updatable = false: bir kez yazıldıktan sonra değiştirilemez.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Son güncelleme zamanı.
     * @LastModifiedDate: Her güncelleme işleminde otomatik güncellenir.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Kaydı kimin oluşturduğu (kullanıcı ID veya sistem adı).
     * @CreatedBy: AuditorAware bean'i ile doldurulur.
     * Alternatif: manuel set etmek — ama AuditingEntityListener otomatik yönetimi sağlar.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * Optimistic Locking için versiyon alanı.
     * @Version: JPA'nın eşzamanlı güncelleme koruması mekanizması.
     * İki kullanıcı aynı kaydı aynı anda güncellemek istediğinde,
     * biri başarılı olur, diğeri OptimisticLockException alır.
     * Finansal işlemlerde (bakiye güncellemesi) kritik öneme sahiptir:
     * "lost update" problemini önler.
     * Alternatif: Pessimistic Locking (@Lock) — veritabanı satırını kilitler,
     * daha güçlü ama daha yavaş ve deadlock riski taşır.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
