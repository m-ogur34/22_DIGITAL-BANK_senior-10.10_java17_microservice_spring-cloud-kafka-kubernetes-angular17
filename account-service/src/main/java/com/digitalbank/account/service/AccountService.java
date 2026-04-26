package com.digitalbank.account.service;

import com.digitalbank.account.dto.AccountResponse;
import com.digitalbank.account.dto.CreateAccountRequest;
import com.digitalbank.account.entity.*;
import com.digitalbank.account.enums.AccountStatus;
import com.digitalbank.account.enums.AccountType;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.common.exception.AccountNotFoundException;
import com.digitalbank.common.util.IbanGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hesap yönetimi iş mantığı.
 *
 * Factory Pattern: createAccount() metodu AccountType'a göre
 * doğru hesap alt sınıfı örneği oluşturur.
 *
 * Cache stratejisi (Redis):
 * Key: "digitalbank:account:balance:{iban}"
 * TTL: 5 dakika
 * Bakiye sorgusu önce cache'e gider, miss olursa DB sorgulanır.
 * Bakiye değiştiğinde (transfer) cache temizlenir (@CacheEvict).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    /**
     * Yeni hesap oluşturur.
     * Factory pattern: AccountType'a göre doğru alt sınıfı oluşturur.
     *
     * @Transactional:
     * - Propagation.REQUIRED (varsayılan): Aktif transaction varsa katıl, yoksa yeni başlat
     * - Isolation.READ_COMMITTED (varsayılan): Dirty read engellenir, phantom read mümkün
     *   Bakiye gibi kritik veriler için READ_COMMITTED yeterli
     *
     * @param request  Hesap oluşturma bilgileri
     * @param ownerId  Hesap sahibinin kullanıcı ID'si
     * @return Oluşturulan hesabın bilgileri
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UUID ownerId) {

        // Müşteri başına maksimum hesap sayısı kontrolü
        if (accountRepository.countByOwnerId(ownerId) >= 10) {
            throw new IllegalStateException("Maksimum hesap sayısına ulaşıldı (10)");
        }

        // Factory: Hesap tipine göre doğru nesneyi oluştur
        BaseAccount account = createAccountByType(request, ownerId);

        // Benzersiz IBAN üret (çakışma olasılığı çok düşük ama kontrol ederiz)
        String iban;
        do {
            iban = IbanGenerator.generate();
        } while (accountRepository.existsByIban(iban));

        account.setIban(iban);
        account.setOwnerId(ownerId);
        account.setAccountName(request.getAccountName());
        account.setCurrency("TRY");

        BaseAccount savedAccount = accountRepository.save(account);
        log.info("Yeni hesap oluşturuldu: IBAN={}, Tip={}, Sahip={}",
                iban, request.getAccountType(), ownerId);

        return mapToResponse(savedAccount);
    }

    /**
     * Hesap bakiyesini sorgular — Redis cache kullanır.
     *
     * @Cacheable: Önce Redis'e bakar (key: balance::IBAN)
     * Cache'de varsa DB'ye gitmez → hızlı yanıt
     * Cache'de yoksa DB'den alır ve cache'e yazar (TTL: 5dk - application.yml'de tanımlı)
     *
     * @param iban     Sorgulanacak hesabın IBAN numarası
     * @param ownerId  Sahiplik kontrolü için — başkasının bakiyesi görülemesin
     */
    @Cacheable(value = "balance", key = "#iban")
    // Cache key format: "balance::TR330006100..." → Redis'te "digitalbank:account:balance:TR33..." olarak saklanır
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String iban, UUID ownerId) {
        log.debug("Cache miss — DB'den bakiye sorgulanıyor: {}", iban);

        BaseAccount account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));

        // Güvenlik: Sahiplik kontrolü — başkası bakiyeni göremez
        if (!account.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Bu hesabın bakiyesini görme yetkiniz yok");
        }

        return account.getBalance();
    }

    /**
     * Kullanıcının tüm aktif hesaplarını listeler.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByOwner(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Hesabı dondurur (EMPLOYEE/ADMIN yetkisi gerekli — controller'da @PreAuthorize ile kontrol edilir).
     * Cache temizlenir.
     *
     * @CacheEvict: Bakiye cache'ini temizler — dondurulmuş hesabın cache'deki değeri geçersiz
     */
    @Transactional
    @CacheEvict(value = "balance", key = "#iban")
    public AccountResponse freezeAccount(String iban) {
        BaseAccount account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));

        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        log.info("Hesap donduruldu: {}", iban);

        return mapToResponse(account);
    }

    /**
     * Dondurulmuş hesabı aktifleştirir.
     */
    @Transactional
    @CacheEvict(value = "balance", key = "#iban")
    public AccountResponse activateAccount(String iban) {
        BaseAccount account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Kapalı hesap aktifleştirilemez");
        }

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Hesap aktifleştirildi: {}", iban);

        return mapToResponse(account);
    }

    /**
     * Hesap bakiyesini günceller (transaction-service tarafından çağrılır).
     *
     * @Transactional(isolation = Isolation.REPEATABLE_READ):
     * Bu işlemde okuduğumuz satır başka bir transaction tarafından değiştirilemez.
     * Bakiye güncellemelerinde kritik: read → check → update arası başka değişiklik olmaz.
     * @Version (optimistic locking) ile birlikte double protection sağlar.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = "balance", key = "#iban")
    public void updateBalance(String iban, BigDecimal newBalance) {
        BaseAccount account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException(iban));
        account.setBalance(newBalance);
        accountRepository.save(account);
        // @Version otomatik artırılır — eşzamanlı güncelleme varsa OptimisticLockException
    }

    /**
     * AccountType'a göre doğru alt sınıf örneği oluşturur.
     * Factory Method Pattern: Nesne oluşturma mantığını bir yerde toplar.
     * switch expression (Java 14+): daha okunabilir, kapsamlı kontrol
     */
    private BaseAccount createAccountByType(CreateAccountRequest request, UUID ownerId) {
        return switch (request.getAccountType()) {
            case VADESIZ -> {
                VadesizHesap hesap = new VadesizHesap();
                yield hesap;
            }
            case VADELI -> {
                VadeliHesap hesap = new VadeliHesap();
                hesap.setVadeGunu(request.getVadeGunu() != null ? request.getVadeGunu() : 90);
                hesap.setFaizOrani(request.getFaizOrani() != null ? request.getFaizOrani() : new BigDecimal("12.00"));
                hesap.setVadeBaslangic(LocalDate.now());
                hesap.setVadeBitis(LocalDate.now().plusDays(hesap.getVadeGunu()));
                yield hesap;
            }
            case TASARRUF -> new TasarrufHesap();
            case YATIRIM -> {
                YatirimHesap hesap = new YatirimHesap();
                hesap.setRiskSeviyesi(request.getRiskSeviyesi() != null ? request.getRiskSeviyesi() : 1);
                yield hesap;
            }
        };
    }

    /**
     * Entity'yi DTO'ya dönüştürür.
     * Polimorfizm: instanceof kontrolleri ile alt sınıf alanlarını da doldurur.
     */
    private AccountResponse mapToResponse(BaseAccount account) {
        AccountResponse.AccountResponseBuilder builder = AccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .accountTypeName(account.getAccountType().getDisplayName())
                .status(account.getStatus())
                .statusName(account.getStatus().getDisplayName())
                .accountName(account.getAccountName())
                .currency(account.getCurrency())
                .ownerId(account.getOwnerId())
                .createdAt(account.getCreatedAt());

        // Alt sınıfa özgü alanları ekle
        if (account instanceof VadeliHesap vadeliHesap) {
            builder.vadeGunu(vadeliHesap.getVadeGunu())
                   .faizOrani(vadeliHesap.getFaizOrani())
                   .vadeBitis(vadeliHesap.getVadeBitis());
        } else if (account instanceof YatirimHesap yatirimHesap) {
            builder.riskSeviyesi(yatirimHesap.getRiskSeviyesi())
                   .portfoyDegeri(yatirimHesap.getPortfoyDegeri());
        }

        return builder.build();
    }
}
