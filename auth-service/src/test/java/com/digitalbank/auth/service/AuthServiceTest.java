package com.digitalbank.auth.service;

import com.digitalbank.auth.dto.LoginRequest;
import com.digitalbank.auth.dto.RegisterRequest;
import com.digitalbank.auth.dto.TokenResponse;
import com.digitalbank.auth.entity.Customer;
import com.digitalbank.auth.entity.Role;
import com.digitalbank.auth.repository.RefreshTokenRepository;
import com.digitalbank.auth.repository.RoleRepository;
import com.digitalbank.auth.repository.UserRepository;
import com.digitalbank.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService birim testleri.
 * @ExtendWith(MockitoExtension.class): JUnit 5 + Mockito entegrasyonu.
 * Mock'lar otomatik inject edilir — Spring context başlatılmaz (hızlı).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Birim Testleri")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Role customerRole;
    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        // Her test öncesi hazırlık
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("Kullanıcı");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Test1234!");
        registerRequest.setTcNo("11111111110"); // Algoritmaya uygun TC No
        registerRequest.setMonthlyIncome(BigDecimal.valueOf(10000));

        customerRole = new Role();
        customerRole.setId(UUID.randomUUID());
        customerRole.setName(Role.RoleName.ROLE_CUSTOMER);

        savedCustomer = new Customer();
        savedCustomer.setId(UUID.randomUUID());
        savedCustomer.setEmail("test@example.com");
        savedCustomer.setFirstName("Test");
        savedCustomer.setLastName("Kullanıcı");
        savedCustomer.setTcNo("11111111110");
        savedCustomer.setRoles(Set.of(customerRole));
    }

    @Test
    @DisplayName("Geçerli bilgilerle kayıt olunca token döndürülmeli")
    void register_withValidData_shouldReturnTokenResponse() {
        // GIVEN (hazırlık)
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByTcNo(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any())).thenReturn(savedCustomer);
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString(), anyList())).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800000L);

        // WHEN (çalıştır)
        TokenResponse response = authService.register(registerRequest);

        // THEN (doğrula)
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        // Kayıt sırasında şifrenin hash'lendiğini doğrula
        verify(passwordEncoder, times(1)).encode("Test1234!");
        // Kullanıcının DB'ye kaydedildiğini doğrula
        verify(userRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Kayıtlı email ile tekrar kayıt olunca hata fırlatılmalı")
    void register_withExistingEmail_shouldThrowException() {
        // GIVEN
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zaten kayıtlı");

        // Kayıt denenmemeli
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hatalı şifre ile giriş yapınca hata fırlatılmalı")
    void login_withWrongPassword_shouldThrowBadCredentials() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("WrongPassword!");

        // AuthenticationManager hata fırlatır → Spring Security'nin davranışını simüle et
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        // Token üretilmemeli
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Geçerli bilgilerle giriş yapılınca token döndürülmeli")
    void login_withValidCredentials_shouldReturnTokenResponse() {
        // GIVEN
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Test1234!");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedCustomer));
        when(jwtUtil.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString(), anyList())).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800000L);

        // WHEN
        TokenResponse response = authService.login(loginRequest);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");
    }
}
