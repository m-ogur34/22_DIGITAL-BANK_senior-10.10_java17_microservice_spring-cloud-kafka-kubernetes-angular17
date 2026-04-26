package com.digitalbank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Transaction Service — Para transferi ve işlem geçmişi yönetimi.
 * Kafka ile event yayını, Elasticsearch ile full-text arama.
 */
@SpringBootApplication(scanBasePackages = {"com.digitalbank.transaction", "com.digitalbank.common"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
