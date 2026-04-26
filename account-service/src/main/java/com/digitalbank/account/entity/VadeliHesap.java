package com.digitalbank.account.entity;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Vadeli mevduat hesabı.
 * Belirli bir süre para kilitlenir, vade sonunda faiz ile ödenir.
 * Vade dolmadan çekim yapılamaz (islemIzniVar = false).
 */
@Getter
@Setter
@Entity
@Table(name = "vadeli_hesaplar", schema = "account_schema")
@DiscriminatorValue("VADELI")
@PrimaryKeyJoinColumn(name = "id")
public class VadeliHesap extends BaseAccount {

    // Vade süresi (gün): 30, 90, 180, 365 gibi
    @Column(name = "vade_gunu", nullable = false)
    private int vadeGunu;

    // Yıllık faiz oranı (yüzde): 12.50 → %12.50
    @Column(name = "faiz_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal faizOrani;

    // Vade başlangıç tarihi
    @Column(name = "vade_baslangic", nullable = false)
    private LocalDate vadeBaslangic;

    // Vade bitiş tarihi (vadeBaslangic + vadeGunu)
    @Column(name = "vade_bitis", nullable = false)
    private LocalDate vadeBitis;

    public VadeliHesap() {
        setAccountType(AccountType.VADELI);
    }

    /**
     * Basit faiz formülü: Anapara × Faiz Oranı × Gün / 365
     * Örn: 10000 TL × %10 × 90 gün / 365 = 246.58 TL
     */
    @Override
    public BigDecimal faizHesapla() {
        // Yıllık faizi günlük faize çevir
        BigDecimal gunlukFaiz = faizOrani
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        // Kalan gün sayısını hesapla
        long kalanGun = ChronoUnit.DAYS.between(LocalDate.now(), vadeBitis);
        if (kalanGun <= 0) kalanGun = vadeGunu; // Vade dolduysa tam süre

        // Faiz = Anapara × günlük faiz × gün sayısı
        return getBalance()
                .multiply(gunlukFaiz)
                .multiply(BigDecimal.valueOf(kalanGun))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Vadeli hesapta işlem izni: Vade dolmuş olmalı VE hesap aktif olmalı.
     * Vade dolmadan para çekilemez (bankacılık prensibi).
     */
    @Override
    public boolean islemIzniVar() {
        boolean vadeGecti = LocalDate.now().isAfter(vadeBitis) || LocalDate.now().isEqual(vadeBitis);
        return getStatus() == AccountStatus.ACTIVE && vadeGecti;
    }
}
