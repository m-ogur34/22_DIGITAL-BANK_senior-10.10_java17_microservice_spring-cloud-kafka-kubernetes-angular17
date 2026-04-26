package com.digitalbank.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer yapılandırması.
 *
 * Topic tasarım kararları:
 * - partitions=3: 3 consumer paralel okuyabilir → throughput artışı
 * - replicas=1: Dev ortamı (prodüksiyonda 3 olmalı — failover için)
 *
 * Serializasyon: JSON — mesajlar insan tarafından okunabilir, tip bilgisi taşır
 * Alternatif: Avro — daha küçük, schema registry gerektirir
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Kafka producer fabrikası.
     * JsonSerializer: Java nesnelerini JSON byte[]'e çevirir.
     * TRUSTED_PACKAGES: Consumer tarafında hangi paketlerin deserialize edileceği.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Mesaj kaybını önlemek: tüm replica'lar onaylayana kadar bekle
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Geçici hata durumunda otomatik yeniden deneme
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        // Toplu gönderim için buffer (ms) — 0 = anında gönder
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Topic'leri otomatik oluştur (yoksa)
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertTopic() {
        return TopicBuilder.name("fraud-alert-events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
