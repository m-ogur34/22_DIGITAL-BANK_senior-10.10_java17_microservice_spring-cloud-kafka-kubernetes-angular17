package com.digitalbank.transaction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * Elasticsearch bağlantı yapılandırması.
 *
 * Spring Data Elasticsearch 5.x (Spring Boot 3.x ile uyumlu):
 * ElasticsearchConfiguration abstract class'ını extend ederiz.
 * clientConfiguration() metodu override edilir.
 *
 * Elasticsearch kullanım amacı:
 * - İşlem geçmişi full-text arama (açıklama, IBAN)
 * - Tarih ve tutar aralığına göre filtreleme
 * - Sayfalı sonuçlar
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String esHost;

    @Value("${elasticsearch.port:9200}")
    private int esPort;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(esHost + ":" + esPort)
                // Bağlantı timeout'ları — ES cevap vermezse uygulama donmasın
                .withConnectTimeout(java.time.Duration.ofSeconds(5))
                .withSocketTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }
}
