package com.digitalbank.account.entity;

import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Vadesiz (çek/tasarruf) hesabı.
 * Anlık para çekme ve yatırma mümkün.
 * Faiz yok veya çok düşük.
 */
@Getter
@Setter
@Entity
@Table(name = "vadesiz_hesaplar", schema = "account_schema")
@DiscriminatorValue("VADESIZ")
@PrimaryKeyJoinColumn(name = "id")
public class VadesizHesap extends BaseAccount {

    // Anlık transfer limiti (tek seferlik)
    @Column(name = "anlik_limit", precision = 15, scale = 2)
    private BigDecimal anlikLimit = new BigDecimal("50000.00");

    // Günlük toplam transfer limiti
    @Column(name = "gunluk_limit", precision = 15, scale = 2)
    private BigDecimal gunlukLimit = new BigDecimal("100000.00");

    public VadesizHesap() {
        setAccountType(AccountType.VADESIZ);
    }

    /**
     * Vadesiz hesapta faiz hesaplanmaz.
     * Bazı bankalar düşük faiz verir ama basitlik için sıfır döndürüyoruz.
     */
    @Override
    public BigDecimal faizHesapla() {
        return BigDecimal.ZERO; // Vadesiz hesapta getiri yok
    }

    /**
     * Vadesiz hesapta işlem izni: hesap aktif olmalı.
     */
    @Override
    public boolean islemIzniVar() {
        return getStatus() == AccountStatus.ACTIVE;
    }
}
