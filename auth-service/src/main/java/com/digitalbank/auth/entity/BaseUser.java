package com.digitalbank.auth.entity;

import com.digitalbank.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Kullanıcı hiyerarşisinin temel soyut sınıfı.
 *
 * OOP Kalıtım (Inheritance) örneği:
 * BaseUser → Customer (müşteri): tcNo, müşteri numarası
 * BaseUser → Employee (çalışan): sicilNo, departman
 *
 * @Inheritance(strategy = InheritanceType.JOINED):
 * Her alt sınıf için ayrı tablo oluşturulur, BaseUser alanları base tabloda durur.
 * Alternatifler:
 * - SINGLE_TABLE: Tüm alt sınıflar tek tabloda, null'lar çok olur
 * - TABLE_PER_CLASS: Her alt sınıf tüm alanlarıyla ayrı tabloda, JOIN gerekmez ama tekrar var
 * JOINED: Normalizasyon açısından en doğru seçenek.
 *
 * @DiscriminatorColumn: Hangi satırın hangi alt sınıfa ait olduğunu belirtir.
 */
@Getter
@Setter
@Entity
@Table(name = "users", schema = "auth_schema",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class BaseUser extends BaseEntity {

    /**
     * Kullanıcının email adresi — giriş yapma kimliği olarak kullanılır.
     * unique = true: aynı email ile iki hesap açılamaz.
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * BCrypt ile hashlenmiş şifre.
     * Neden BCrypt? Tuz (salt) içerir → rainbow table saldırılarına karşı korumalı.
     * work factor ayarlanabilir → donanım güçlendikçe artırılabilir.
     * Asla plain text saklamayız!
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    // Hesap aktif mi? Dondurulmuş hesaplar false
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Email doğrulaması yapıldı mı?
    @Column(name = "is_email_verified")
    private boolean emailVerified = false;

    /**
     * Kullanıcı rolleri — çoka çok ilişki (bir kullanıcının birden fazla rolü olabilir).
     * FetchType.EAGER: Kullanıcı yüklendiğinde rolleri de yükle (Spring Security için gerekli).
     * Alternatif LAZY: Roller gerektiğinde yüklenir — performans açısından daha iyi ama
     * Hibernate session kapalıysa LazyInitializationException hatası verir.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        schema = "auth_schema",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
