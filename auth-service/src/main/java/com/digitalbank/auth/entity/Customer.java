package com.digitalbank.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Banka müşterisini temsil eder.
 * BaseUser'dan kalıtım alır — ortak alanlar (email, şifre vb.) orada tanımlı.
 *
 * @DiscriminatorValue("CUSTOMER"): users tablosundaki user_type sütununda
 * bu sınıf için "CUSTOMER" değeri kullanılır.
 *
 * @PrimaryKeyJoinColumn: customers tablosunun id sütunu, users tablosunun id'sine FK.
 * Bu JOINED inheritance stratejisinin çalışma şeklidir.
 */
@Getter
@Setter
@Entity
@Table(name = "customers", schema = "auth_schema",
    indexes = {
        @Index(name = "idx_customers_tc_no", columnList = "tc_no", unique = true),
        @Index(name = "idx_customers_customer_no", columnList = "customer_no", unique = true)
    }
)
@DiscriminatorValue("CUSTOMER")
@PrimaryKeyJoinColumn(name = "id")
public class Customer extends BaseUser {

    /**
     * TC Kimlik Numarası — Türk bankacılığında zorunlu.
     * ValidationUtils.isValidTcNo() ile doğrulanır.
     * 11 karakter, unique.
     */
    @Column(name = "tc_no", nullable = false, unique = true, length = 11)
    private String tcNo;

    /**
     * Müşteri numarası — sistemin ürettiği benzersiz numara.
     * Müşteri hizmetlerinde "hesap numaranız" olarak kullanılır.
     * Format: DB000001, DB000002 gibi
     */
    @Column(name = "customer_no", nullable = false, unique = true, length = 20)
    private String customerNo;

    /**
     * Telefon numarası — bildirim ve 2FA için.
     */
    @Column(name = "phone", length = 15)
    private String phone;

    /**
     * Aylık net gelir — kredi skoru hesaplamasında kullanılır.
     * BigDecimal: finansal hassasiyet için (float/double kullanmıyoruz).
     */
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private java.math.BigDecimal monthlyIncome;
}
