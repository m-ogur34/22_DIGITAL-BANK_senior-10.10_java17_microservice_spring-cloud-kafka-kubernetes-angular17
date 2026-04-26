package com.digitalbank.auth.config;

import com.digitalbank.auth.filter.JwtAuthenticationFilter;
import com.digitalbank.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 yapılandırması.
 *
 * Spring Security 6'daki değişiklikler (Spring Security 5'ten farklı):
 * - WebSecurityConfigurerAdapter kaldırıldı → SecurityFilterChain bean'i kullanılır
 * - Lambda DSL zorunlu hale geldi
 * - CSRF varsayılan olarak aktif — REST API için devre dışı bırakıyoruz
 *
 * Filter chain sırası (önemli!):
 * 1. JwtAuthenticationFilter (bizim custom filter)
 * 2. UsernamePasswordAuthenticationFilter (Spring Security'nin login filtresi)
 * 3. ... diğer Spring Security filtreleri
 *
 * @EnableMethodSecurity: @PreAuthorize anotasyonlarını aktif eder.
 * Örn: @PreAuthorize("hasRole('ADMIN')") ile metod bazlı yetkilendirme.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Ana güvenlik filtre zinciri.
     * Her HTTP isteği bu zincirden geçer.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery): Session tabanlı uygulamalarda kritik.
            // REST API + JWT kullandığımız için CSRF'ye gerek yok — stateless mimari.
            .csrf(AbstractHttpConfigurer::disable)

            // CORS: Angular frontend'in farklı port'tan istek atmasına izin ver
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // İstek yetkilendirme kuralları
            .authorizeHttpRequests(auth -> auth
                // Public endpoint'ler: kimlik doğrulama gerektirmez
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/refresh",
                    "/actuator/health",          // Kubernetes liveness probe
                    "/actuator/health/readiness" // Kubernetes readiness probe
                ).permitAll()
                // Admin endpoint'leri: yalnızca ADMIN rolü
                .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                // Diğer tüm istekler: kimlik doğrulama zorunlu
                .anyRequest().authenticated()
            )

            // Session politikası: STATELESS — her istek kendi token'ını taşır
            // Session oluşturulmaz → sunucu tarafında state tutulmaz → ölçeklenebilir
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authentication provider: hangi UserDetailsService ve PasswordEncoder kullanılacak
            .authenticationProvider(authenticationProvider())

            // Custom JWT filtremizi UsernamePasswordAuthenticationFilter'dan ÖNCE ekle
            // Neden önce? JWT token varsa, Spring Security'nin login formunu atla
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider: Veritabanından kullanıcı yükleyerek kimlik doğrular.
     * UserDetailsService + PasswordEncoder kombinasyonu ile çalışır.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt şifre encoder.
     * strength = 10 (varsayılan): 2^10 = 1024 iterasyon
     * Artırılabilir (12, 14) — daha güvenli ama yavaş.
     * Saldırgan şifre listesi denerken her deneme ~100ms sürer → brute force engellenir.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager: Spring Security'nin kimlik doğrulama orkestratörü.
     * AuthService'de direct authentication için kullanılır.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS yapılandırması.
     * Angular frontend'in localhost:4200'den istek atmasına izin verir.
     * Prodüksiyon'da gerçek domain adresleri yazılmalıdır.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // İzin verilen origin'ler: Angular dev server + prodüksiyon domain'i
        config.setAllowedOrigins(List.of(
            "http://localhost:4200",
            "http://localhost:3000",
            "https://digitalbank.com"
        ));
        // İzin verilen HTTP metodlar
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // İzin verilen header'lar: Authorization header kritik (JWT için)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        // Cookie/credential gönderimini aktif et
        config.setAllowCredentials(true);
        // Preflight isteği cache süresi (saniye)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Tüm path'lere CORS kuralını uygula
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
