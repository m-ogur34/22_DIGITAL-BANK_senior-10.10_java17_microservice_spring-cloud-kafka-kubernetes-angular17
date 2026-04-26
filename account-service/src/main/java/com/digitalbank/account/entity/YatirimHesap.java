package com.digitalbank.account.entity;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Yatırım hesabı.
 * Hisse senedi, yatırım fonu alım satımı için.
 * Faiz yok — değer artışı ve temettü hedeflenir.
 */
@Getter
@Setter
@Entity
@Table(name = "yatirim_hesaplar", schema = "account_schema")
@DiscriminatorValue("YATIRIM")
@PrimaryKeyJoinColumn(name = "id")
public class YatirimHesap extends BaseAccount {

    /**
     * Risk seviyesi: 1 (düşük) → 5 (yüksek)
     * 1: Para piyasası fonları (düşük risk, düşük getiri)
     * 5: Bireysel hisse senetleri (yüksek risk, yüksek potansiyel getiri)
     */
    @Column(name = "risk_seviyesi", nullable = false)
    private int riskSeviyesi = 1;

    // Portföy toplam değeri (TL) — piyasa fiyatına göre güncellenir
    @Column(name = "portfoy_degeri", precision = 15, scale = 2)
    private BigDecimal portfoyDegeri = BigDecimal.ZERO;

    // Yatırım varlıkları (JSON formatında saklanır — gerçek sistemde ayrı tablo)
    @Column(name = "varliklar_json", columnDefinition = "TEXT")
    private String varliklarJson;

    public YatirimHesap() {
        setAccountType(AccountType.YATIRIM);
    }

    /**
     * Yatırım hesabında garantili faiz yok.
     * Portföy değeri piyasa koşullarına göre değişir.
     * Gerçek sistemde: portföy pozisyonları API'den çekilir.
     */
    @Override
    public BigDecimal faizHesapla() {
        return BigDecimal.ZERO; // Faiz yok, yatırım getirisi ayrı hesaplanır
    }

    /**
     * Yatırım hesabında işlem: Hesap aktif olmalı.
     * Piyasa saatleri kontrolü gerçek sistemde eklenir.
     */
    @Override
    public boolean islemIzniVar() {
        return getStatus() == AccountStatus.ACTIVE;
    }
}
