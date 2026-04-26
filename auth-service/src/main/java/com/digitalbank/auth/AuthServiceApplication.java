package com.digitalbank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auth Service başlangıç noktası.
 *
 * @SpringBootApplication: @Configuration + @EnableAutoConfiguration + @ComponentScan'ı birleştirir.
 * ComponentScan: com.digitalbank.auth ve com.digitalbank.common paketlerini tararız;
 * bu sayede GlobalExceptionHandler ve JwtUtil gibi common-lib bean'leri bu serviste de aktif olur.
 *
 * @EnableJpaAuditing: BaseEntity'deki @CreatedDate, @LastModifiedDate otomatik doldurulur.
 * @EnableScheduling: Refresh token temizleme job'u için gereklidir.
 */
@SpringBootApplication(scanBasePackages = {"com.digitalbank.auth", "com.digitalbank.common"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
