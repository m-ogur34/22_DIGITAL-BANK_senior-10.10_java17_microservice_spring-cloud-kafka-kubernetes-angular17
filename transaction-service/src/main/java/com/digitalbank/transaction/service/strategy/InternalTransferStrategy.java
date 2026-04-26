package com.digitalbank.transaction.service.strategy;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.enums.TransactionType;
import com.digitalbank.transaction.repository.TransactionRepository;
import com.digitalbank.common.exception.AccountNotFoundException;
import com.digitalbank.common.exception.InsufficientFundsException;
import com.digitalbank.common.exception.DailyLimitExceededException;
import com.digitalbank.common.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/**
 * Aynı banka içi transfer stratejisi.
 *
 * Akış:
 * 1. Günlük limit kontrolü (Redis counter)
 * 2. Bakiye kontrolü (account-service'den gerçek bakiye)
 * 3. DEBIT + CREDIT çift kayıt (atomik @Transactional)
 * 4. Bakiye güncelleme
 * 5. Elasticsearch'e kayıt
 * 6. Kafka'ya event yayını
 *
 * @Transactional(isolation = SERIALIZABLE):
 * En güçlü izolasyon seviyesi — phantom read ve dirty read tamamen engellenir.
 * Finansal işlemler için idealdir ancak performance maliyeti yüksektir.
 * Alternatif: REPEATABLE_READ + optimistic locking (@Version) = daha performanslı
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalTransferStrategy implements TransferStrategy {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Günlük limit: 100.000 TL
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("100000.00");

    // Redis key format: "digitalbank:transaction:daily-limit:{userId}:{date}"
    private static final String DAILY_LIMIT_KEY = "digitalbank:transaction:daily-limit:%s:%s";

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    // REPEATABLE_READ: Aynı satırı tekrar okuduğumuzda başka transaction değiştirmiş olamaz
    // + @Version ile optimistic locking: double protection
    public TransferResponse execute(TransferRequest request) {

        String referenceId = UUID.randomUUID().toString();
        log.info("Banka içi transfer başlatıldı: {} -> {} ({})",
                request.getSenderIban(), request.getReceiverIban(), referenceId);

        // 1. Günlük limit kontrolü
        checkDailyLimit(request.getOwnerId(), request.getAmount());

        // 2. Bakiye burada doğrulanır (transaction-service kendi DB'sinden sorgular)
        //    Gerçek sistemde account-service HTTP call veya shared event ile yapılır
        //    Bu simülasyonda bakiyeyi transfer isteğinden alıyoruz
        if (!MoneyUtils.isSufficient(request.getCurrentBalance(), request.getAmount())) {
            // FAILED kaydı yaz
            saveFailedTransaction(request, referenceId, "Yetersiz bakiye");
            throw new InsufficientFundsException(request.getSenderIban());
        }

        // 3. DEBIT kaydı — gönderen hesap
        Transaction debitTxn = Transaction.builder()
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .amount(request.getAmount())
                .description(request.getDescription())
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .ownerId(request.getOwnerId())
                .referenceId(referenceId)
                .internal(true)
                .build();

        // 4. CREDIT kaydı — alıcı hesap
        Transaction creditTxn = Transaction.builder()
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .amount(request.getAmount())
                .description(request.getDescription())
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .ownerId(request.getOwnerId())
                .referenceId(referenceId)
                .internal(true)
                .build();

        // Atomik kayıt: ikisi birlikte commit olur ya da ikisi de olmaz
        Transaction savedDebit = transactionRepository.save(debitTxn);
        Transaction savedCredit = transactionRepository.save(creditTxn);

        // 5. Günlük limit sayacını artır
        incrementDailyLimitCounter(request.getOwnerId(), request.getAmount());

        log.info("Transfer tamamlandı: referenceId={}", referenceId);

        return TransferResponse.builder()
                .transactionId(savedDebit.getId().toString())
                .referenceId(referenceId)
                .status(TransactionStatus.COMPLETED)
                .message("Transfer başarıyla tamamlandı")
                .amount(request.getAmount())
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .build();
    }

    /**
     * Redis'te günlük harcama toplamını kontrol eder.
     * Key: "digitalbank:transaction:daily-limit:{userId}:{tarih}"
     * TTL: Gece yarısına kadar (24 saat)
     *
     * Örn: Kullanıcı bugün 80.000 TL transfer ettiyse, 25.000 TL daha transfere çalışırsa reddedilir.
     */
    private void checkDailyLimit(String userId, BigDecimal amount) {
        String today = java.time.LocalDate.now().toString();
        String key = String.format(DAILY_LIMIT_KEY, userId, today);

        Object currentObj = redisTemplate.opsForValue().get(key);
        BigDecimal currentTotal = currentObj != null
                ? new BigDecimal(currentObj.toString())
                : BigDecimal.ZERO;

        BigDecimal newTotal = MoneyUtils.add(currentTotal, amount);

        if (newTotal.compareTo(DAILY_LIMIT) > 0) {
            log.warn("Günlük limit aşıldı: kullanıcı={}, toplam={}", userId, newTotal);
            throw new DailyLimitExceededException(DAILY_LIMIT);
        }
    }

    /**
     * Transfer sonrası günlük limit sayacını Redis'te artırır.
     * TTL: 24 saat sonra otomatik sıfırlanır.
     */
    private void incrementDailyLimitCounter(String userId, BigDecimal amount) {
        String today = java.time.LocalDate.now().toString();
        String key = String.format(DAILY_LIMIT_KEY, userId, today);

        Object currentObj = redisTemplate.opsForValue().get(key);
        BigDecimal current = currentObj != null ? new BigDecimal(currentObj.toString()) : BigDecimal.ZERO;
        BigDecimal newValue = MoneyUtils.add(current, amount);

        // TTL: bugün gece yarısına kadar
        redisTemplate.opsForValue().set(key, newValue.toPlainString(), Duration.ofHours(24));
    }

    private void saveFailedTransaction(TransferRequest request, String referenceId, String reason) {
        Transaction failedTxn = Transaction.builder()
                .senderIban(request.getSenderIban())
                .receiverIban(request.getReceiverIban())
                .amount(request.getAmount())
                .description(request.getDescription())
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.FAILED)
                .ownerId(request.getOwnerId())
                .referenceId(referenceId)
                .failureReason(reason)
                .internal(true)
                .build();
        transactionRepository.save(failedTxn);
    }
}
