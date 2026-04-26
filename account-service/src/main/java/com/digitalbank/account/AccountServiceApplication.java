package com.digitalbank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Account Service başlangıç noktası.
 * Bu servis JWT'yi kendi filter chain'iyle doğrular.
 * Bakiye cache için @EnableCaching aktif edilmiştir.
 */
@SpringBootApplication(scanBasePackages = {"com.digitalbank.account", "com.digitalbank.common"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableCaching
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
