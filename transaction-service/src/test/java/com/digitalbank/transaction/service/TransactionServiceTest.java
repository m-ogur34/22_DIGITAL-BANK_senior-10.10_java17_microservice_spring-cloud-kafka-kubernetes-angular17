package com.digitalbank.transaction.service;

import com.digitalbank.transaction.dto.TransferRequest;
import com.digitalbank.transaction.dto.TransferResponse;
import com.digitalbank.transaction.entity.Transaction;
import com.digitalbank.transaction.enums.TransactionStatus;
import com.digitalbank.transaction.kafka.TransactionEventProducer;
import com.digitalbank.transaction.repository.TransactionRepository;
import com.digitalbank.transaction.repository.TransactionSearchRepository;
import com.digitalbank.transaction.saga.TransferSaga;
import com.digitalbank.transaction.service.strategy.InternalTransferStrategy;
import com.digitalbank.transaction.service.strategy.TransferContext;
import com.digitalbank.common.exception.DailyLimitExceededException;
import com.digitalbank.common.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionService birim testleri.
 * Transfer senaryoları: başarılı, yetersiz bakiye, günlük limit aşımı.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Birim Testleri")
class TransactionServiceTest {

    @Mock private TransferSaga transferSaga;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSearchRepository searchRepository;
    @Mock private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private TransactionService transactionService;

    private TransferRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TransferRequest();
        validRequest.setSenderIban("TR330006100012345678901234");
        validRequest.setReceiverIban("TR330006100098765432109876");
        validRequest.setAmount(new BigDecimal("500.00"));
        validRequest.setDescription("Test transfer");
        validRequest.setCurrentBalance(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Geçerli transfer isteği başarıyla tamamlanmalı")
    void transfer_withValidRequest_shouldComplete() {
        // GIVEN
        TransferResponse mockResponse = TransferResponse.builder()
                .transactionId(UUID.randomUUID().toString())
                .referenceId(UUID.randomUUID().toString())
                .status(TransactionStatus.COMPLETED)
                .amount(validRequest.getAmount())
                .senderIban(validRequest.getSenderIban())
                .receiverIban(validRequest.getReceiverIban())
                .build();

        when(transferSaga.execute(any(TransferRequest.class))).thenReturn(mockResponse);

        // WHEN
        TransferResponse response = transactionService.transfer(validRequest, "user-123");

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(transferSaga, times(1)).execute(any());
    }

    @Test
    @DisplayName("Yetersiz bakiye durumunda InsufficientFundsException fırlatılmalı")
    void transfer_withInsufficientBalance_shouldThrowException() {
        // GIVEN
        when(transferSaga.execute(any(TransferRequest.class)))
                .thenThrow(new InsufficientFundsException(validRequest.getSenderIban()));

        // WHEN & THEN
        assertThatThrownBy(() -> transactionService.transfer(validRequest, "user-123"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Yetersiz");
    }

    @Test
    @DisplayName("Günlük limit aşımında DailyLimitExceededException fırlatılmalı")
    void transfer_whenDailyLimitExceeded_shouldThrowException() {
        // GIVEN — 100.001 TL transfer → limit aşımı
        validRequest.setAmount(new BigDecimal("100001.00"));
        validRequest.setCurrentBalance(new BigDecimal("500000.00"));

        when(transferSaga.execute(any()))
                .thenThrow(new DailyLimitExceededException(new BigDecimal("100000.00")));

        // WHEN & THEN
        assertThatThrownBy(() -> transactionService.transfer(validRequest, "user-123"))
                .isInstanceOf(DailyLimitExceededException.class)
                .hasMessageContaining("limit");
    }

    @Test
    @DisplayName("Aynı IBAN'a transfer denemesi reddedilmeli")
    void transfer_toSameIban_shouldBeRejected() {
        // GIVEN — Gönderen = Alıcı (kendi hesabına gönderme)
        validRequest.setReceiverIban(validRequest.getSenderIban());

        when(transferSaga.execute(any()))
                .thenThrow(new IllegalArgumentException("Aynı hesaba transfer yapılamaz"));

        // WHEN & THEN
        assertThatThrownBy(() -> transactionService.transfer(validRequest, "user-123"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
