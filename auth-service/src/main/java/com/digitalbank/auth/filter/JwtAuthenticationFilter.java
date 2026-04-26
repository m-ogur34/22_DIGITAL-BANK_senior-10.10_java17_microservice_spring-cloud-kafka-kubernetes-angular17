package com.digitalbank.auth.filter;

import com.digitalbank.common.exception.InvalidTokenException;
import com.digitalbank.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Her HTTP isteğinde çalışan JWT doğrulama filtresi.
 *
 * OncePerRequestFilter: Her istek için YalnIZCA bir kez çalışmasını garanti eder.
 * Spring Security filter chain'ine SecurityConfig'de eklenir.
 *
 * Filtre akışı:
 * İstek gelir
 *   → Authorization header var mı? (Bearer <token>)
 *     → Hayır: filtreden geç (public endpoint olabilir — SecurityConfig kararı verir)
 *     → Evet: Token doğrula → Claims'ten kullanıcı bilgilerini al → SecurityContext'e yaz
 *       → Hatalı token: 401 döndür
 *
 * Bu mimari sayede her servis JWT'yi bağımsız olarak doğrular.
 * Merkezi Gateway yoktur — Spring Security derinlemesine öğrenilir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // Authorization header adı ve Bearer prefix'i
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization header'ı al
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 2. Header yoksa veya Bearer ile başlamıyorsa, filtreden geç
        //    SecurityConfig'deki permitAll() public endpoint'lere erişim izni verir
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. "Bearer " prefix'ini kaldırarak token string'ini al (7 karakter)
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 4. Token geçerli mi? (imza, süre kontrolü)
            if (jwtUtil.isTokenValid(token)) {

                // 5. Token'dan kullanıcı bilgilerini çıkar
                String userId = jwtUtil.extractUserId(token);
                String email = jwtUtil.extractEmail(token);
                List<String> roles = jwtUtil.extractRoles(token);

                // 6. SecurityContext zaten dolu değilse Authentication oluştur
                //    (iç içe filter chain durumunda çift set'i önler)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {

                    // Rol string'lerini GrantedAuthority listesine çevir
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // UsernamePasswordAuthenticationToken: Spring Security'nin kimlik doğrulama nesnesi
                    // principal: kullanıcı ID veya email (servis tarafından kullanılır)
                    // credentials: null (zaten token ile doğrulandı)
                    // authorities: roller
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                userId,   // principal olarak userId kullanıyoruz
                                null,     // credentials — token doğrulandı, şifre gerekmez
                                authorities
                            );

                    // İstek detaylarını (IP, session ID) authentication nesnesine ekle
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. SecurityContext'e authentication'ı yaz
                    //    Bu noktadan sonra @PreAuthorize, @AuthenticationPrincipal vb. çalışır
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT doğrulandı — kullanıcı: {}, roller: {}", email, roles);
                }
            }
        } catch (InvalidTokenException e) {
            // Token geçersiz — SecurityContext temizle, istek zinciri devam etsin
            // SecurityConfig'deki authorize kuralları erişimi reddedecek
            log.warn("Geçersiz JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("JWT filter hatası: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 8. Bir sonraki filtreye devam et
        filterChain.doFilter(request, response);
    }
}
