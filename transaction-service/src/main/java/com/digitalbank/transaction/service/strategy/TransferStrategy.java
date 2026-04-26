package com.digitalbank.transaction.service.strategy;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;

/**
 * Transfer stratejisi arayüzü — Strategy Pattern.
 *
 * Strategy Pattern: Algoritmanın arayüzini sabit tutarken
 * implementasyonu runtime'da değiştirmeye olanak tanır.
 *
 * Kullanım durumları:
 * - InternalTransferStrategy: Aynı banka içi transfer
 * - ExternalTransferStrategy: Farklı banka (EFT/SWIFT simülasyonu)
 *
 * TransferContext IBAN prefix'ine bakarak strateji seçer (Factory).
 * İleride yeni strateji eklemek için sadece yeni class yazılır,
 * mevcut kod değiştirilmez (Open/Closed Principle).
 */
public interface TransferStrategy {

    /**
     * Transfer işlemini gerçekleştirir.
     *
     * @param request  Transfer bilgileri (gönderen, alıcı IBAN, tutar, açıklama)
     * @return Transfer sonucu (başarı/hata, işlem ID'si)
     */
    TransferResponse execute(TransferRequest request);
}
