package com.digitalbank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Notification Service — Kafka mesajlarını dinler, bildirim geçmişi kaydeder.
 * Dışarıya açık REST endpoint yoktur — yalnızca Kafka consumer.
 * JWT doğrulaması yapılmaz (dışarıdan HTTP isteği almaz).
 */
@SpringBootApplication(scanBasePackages = {"com.digitalbank.notification", "com.digitalbank.common"})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
