package com.digitalbank.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import java.util.Optional;

@Configuration
public class AppConfig {
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("KAFKA_CONSUMER");
    }
}
