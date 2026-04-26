package com.digitalbank.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Genel uygulama konfigürasyonu.
 * JPA Auditing için AuditorAware bean'i sağlar.
 */
@Configuration
public class AppConfig {

    /**
     * AuditorAware: BaseEntity'deki @CreatedBy alanını doldurmak için
     * SecurityContext'ten mevcut kullanıcıyı döndürür.
     * Kullanıcı giriş yapmamışsa "SYSTEM" döner.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(authentication.getName());
        };
    }
}
