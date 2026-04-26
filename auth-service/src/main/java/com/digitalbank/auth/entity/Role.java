package com.digitalbank.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Kullanıcı rollerini temsil eden entity.
 * Spring Security'de GrantedAuthority olarak kullanılır.
 *
 * Roller: ROLE_CUSTOMER, ROLE_EMPLOYEE, ROLE_ADMIN
 * Spring Security "ROLE_" prefix'ini otomatik olarak @PreAuthorize'da tanır.
 */
@Getter
@Setter
@Entity
@Table(name = "roles", schema = "auth_schema")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Rol adı — Spring Security formatı: "ROLE_CUSTOMER"
     * @Enumerated(STRING): Veritabanında string olarak saklanır (daha okunabilir).
     * Alternatif: EnumType.ORDINAL — sayısal değer, ama enum sırası değişirse sorun olur!
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 30)
    private RoleName name;

    /**
     * Roller enum'u.
     * ROLE_ prefix'i Spring Security convention'ıdır.
     */
    public enum RoleName {
        /** Normal banka müşterisi — hesap görüntüleme, transfer yapma */
        ROLE_CUSTOMER,

        /** Banka çalışanı — hesap dondurma/aktifleştirme, müşteri görüntüleme */
        ROLE_EMPLOYEE,

        /** Sistem yöneticisi — tüm işlemler, kullanıcı yönetimi */
        ROLE_ADMIN
    }
}
