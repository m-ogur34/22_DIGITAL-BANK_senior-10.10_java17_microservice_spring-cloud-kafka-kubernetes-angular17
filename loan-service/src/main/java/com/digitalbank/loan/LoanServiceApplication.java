package com.digitalbank.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Loan Service — Kredi başvuru, skor hesaplama, taksit planı.
 * Decorator pattern ile esnek kredi ücret hesaplama.
 */
@SpringBootApplication(scanBasePackages = {"com.digitalbank.loan", "com.digitalbank.common"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class LoanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
