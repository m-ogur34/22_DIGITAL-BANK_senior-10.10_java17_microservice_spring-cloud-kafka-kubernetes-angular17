package com.digitalbank.account.entity;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tasarruf hesabı.
 * Düzenli birikim için — orta düzey faiz, para çekim sınırı var.
 */
@Getter
@Setter
@Entity
@Table(name = "tasarruf_hesaplar", schema = "account_schema")
@DiscriminatorValue("TASARRUF")
@PrimaryKeyJoinColumn(name = "id")
public class TasarrufHesap extends BaseAccount {

    // Aylık faiz oranı (yüzde)
    @Column(name = "aylik_faiz_orani", precision = 5, scale = 2)
    private BigDecimal aylikFaizOrani = new BigDecimal("0.50"); // %0.50 aylık

    // Aylık maksimum çekim sayısı
    @Column(name = "aylik_max_cekim")
    private int aylikMaxCekim = 6;

    // Bu ay kaç çekim yapıldı?
    @Column(name = "bu_ay_cekim_sayisi")
    private int buAyCekimSayisi = 0;

    public TasarrufHesap() {
        setAccountType(AccountType.TASARRUF);
    }

    /**
     * Aylık faiz hesaplama.
     * Bakiye × Aylık faiz oranı
     */
    @Override
    public BigDecimal faizHesapla() {
        return getBalance()
                .multiply(aylikFaizOrani.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tasarruf hesabında işlem izni:
     * - Hesap aktif olmalı
     * - Aylık çekim limitini aşmamış olmalı
     */
    @Override
    public boolean islemIzniVar() {
        return getStatus() == AccountStatus.ACTIVE
                && buAyCekimSayisi < aylikMaxCekim;
    }
}
