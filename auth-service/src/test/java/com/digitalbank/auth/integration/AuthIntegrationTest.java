package com.digitalbank.auth.integration;

import com.digitalbank.auth.dto.LoginRequest;
import com.digitalbank.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth Service entegrasyon testleri.
 *
 * @SpringBootTest: Tam Spring context başlatır — gerçek servisler, gerçek DB
 * @Testcontainers: Test sırasında gerçek PostgreSQL Docker container başlatır
 * @AutoConfigureMockMvc: HTTP isteklerini gerçek sunucu başlatmadan test eder
 *
 * Bu testler birim testlerden yavaştır ama daha güvenilirdir:
 * - Gerçek Flyway migration çalışır
 * - Gerçek BCrypt encoding test edilir
 * - Gerçek JWT üretimi test edilir
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Auth Service Entegrasyon Testleri")
class AuthIntegrationTest {

    // Testcontainers: Her test sınıfı için bir kez PostgreSQL container başlatır
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("digitalbank_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Testcontainers'ın dinamik portunu Spring'e aktarır.
     * Container başlamadan URL bilinmediği için @DynamicPropertySource kullanılır.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> postgres.getJdbcUrl() + "?currentSchema=auth_schema");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("Geçerli kayıt isteği 201 Created ve token döndürmeli")
    void register_withValidRequest_shouldReturn201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Integration");
        request.setLastName("Test");
        request.setEmail("integration@test.com");
        request.setPassword("Test1234!");
        request.setTcNo("11111111110");
        request.setMonthlyIncome(BigDecimal.valueOf(10000));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("Eksik alan ile kayıt isteği 400 döndürmeli")
    void register_withMissingFields_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        // firstName, tcNo vs eksik

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
