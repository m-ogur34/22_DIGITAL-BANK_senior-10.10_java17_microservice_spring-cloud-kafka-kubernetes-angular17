package com.digitalbank.notification.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer yapılandırması.
 *
 * Consumer group: "notification-group"
 * Aynı group'taki consumer'lar partition'ları paylaşır.
 * 3 partition → 3 consumer eşzamanlı çalışabilir (throughput artışı).
 *
 * AUTO_OFFSET_RESET: "earliest" — servis yeniden başlatıldığında
 * işlenmemiş mesajları baştan okur (kayıp önlenir).
 * Alternatif: "latest" — sadece yeni mesajları okur
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Güvenilir paketler: bu paketlerden gelen JSON sınıfları deserialize edilir
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.digitalbank.*, java.util.*");
        // Map<String, Object> olarak deserialize et (type-safe)
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        // Baştan oku — mesaj kaybı önlenir
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Manuel commit yerine otomatik — basit ve yeterli
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Eşzamanlı consumer sayısı: partition sayısı kadar
        factory.setConcurrency(3);
        return factory;
    }
}
