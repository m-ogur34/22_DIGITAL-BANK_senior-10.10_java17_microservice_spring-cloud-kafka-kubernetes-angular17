package com.digitalbank.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis bağlantı ve şablon yapılandırması.
 *
 * Redis kullanım alanları (auth-service'de):
 * 1. Token blacklist: Çıkış yapılan access token'ın JTI'si → TTL = token kalan süresi
 * 2. Şifre sıfırlama token'ı: 15 dakika TTL
 *
 * Redis key stratejisi (standart format):
 * "digitalbank:{servis}:{tip}:{id}"
 * Örn: "digitalbank:auth:blacklist:abc-jti-123"
 *       "digitalbank:auth:pwd-reset:user@email.com"
 *
 * Neden Redis?
 * - In-memory → çok hızlı (milisaniye altı)
 * - TTL desteği → otomatik expiry
 * - Dağıtık → birden fazla servis instance'ında paylaşılabilir
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redis bağlantı factory — Lettuce client kullanır.
     * Lettuce: non-blocking, thread-safe → Jedis'e göre daha modern.
     * Jedis: blocking, thread-per-connection — eski projeler için.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate: Redis veri tipleriyle çalışmak için ana sınıf.
     * Key: String serializer (insan tarafından okunabilir key'ler)
     * Value: JSON serializer (Object değerlerini JSON'a çevirir)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializasyonu: "digitalbank:auth:blacklist:abc" formatında string
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializasyonu: Java nesnelerini JSON olarak sakla
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
