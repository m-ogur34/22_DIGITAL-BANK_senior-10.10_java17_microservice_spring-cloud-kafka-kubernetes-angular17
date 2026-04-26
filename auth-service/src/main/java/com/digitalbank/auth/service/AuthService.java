package com.digitalbank.auth.service;

import com.digitalbank.auth.dto.*;
import com.digitalbank.auth.entity.*;
import com.digitalbank.auth.repository.*;
import com.digitalbank.common.exception.InvalidTokenException;
import com.digitalbank.common.exception.UserNotFoundException;
import com.digitalbank.common.security.JwtUtil;
import com.digitalbank.common.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kimlik doğrulama ve kullanıcı yönetimi iş mantığı.
 *
 * Sorumluluklar:
 * - Kullanıcı kaydı (kayıt validasyonu, şifre hashleme, IBAN üretimi)
 * - Giriş yapma (Spring Security AuthenticationManager ile)
 * - Token üretimi (access + refresh)
 * - Token yenileme
 * - Çıkış yapma (refresh token iptali, access token blacklist)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Yeni müşteri kaydı işlemi.
     *
     * @Transactional: İşlem başarısız olursa (örn. email zaten kayıtlı)
     * tüm değişiklikler geri alınır. Propagation.REQUIRED varsayılan —
     * varolan transaction'a katılır, yoksa yenisini başlatır.
     *
     * @param request  Kayıt formundan gelen kullanıcı bilgileri
     * @return Başarılı kayıt sonrası access ve refresh token'ları
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {

        // TC kimlik no algoritma doğrulaması — sahte TC ile kayıt önlenir
        if (!ValidationUtils.isValidTcNo(request.getTcNo())) {
            throw new IllegalArgumentException("Geçersiz TC Kimlik Numarası");
        }

        // Email tekrar eden kayıt kontrolü
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu email adresi zaten kayıtlı");
        }

        // TC No tekrar eden kayıt kontrolü
        if (userRepository.existsByTcNo(request.getTcNo())) {
            throw new IllegalArgumentException("Bu TC Kimlik No ile zaten hesap açılmış");
        }

        // Müşteri entity'si oluştur
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        // Şifreyi BCrypt ile hashle — asla plain text saklama!
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setTcNo(request.getTcNo());
        customer.setPhone(request.getPhone());
        customer.setMonthlyIncome(request.getMonthlyIncome());
        // Müşteri numarası: DB<8 haneli UUID başlangıcı>
        customer.setCustomerNo("DB" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());

        // Varsayılan rol ata: ROLE_CUSTOMER
        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER bulunamadı — seed data eksik"));
        customer.getRoles().add(customerRole);

        // Kaydet
        Customer savedCustomer = (Customer) userRepository.save(customer);
        log.info("Yeni müşteri kaydedildi: {} ({})", savedCustomer.getEmail(), savedCustomer.getId());

        // Token üret ve döndür
        return generateTokenResponse(savedCustomer);
    }

    /**
     * Kullanıcı giriş işlemi.
     *
     * Spring Security AuthenticationManager flow:
     * 1. UsernamePasswordAuthenticationToken → kimlik bilgileri taşır
     * 2. AuthenticationManager.authenticate() → DaoAuthenticationProvider'a devretir
     * 3. DaoAuthenticationProvider → UserDetailsService.loadUserByUsername()
     * 4. BCrypt ile şifre karşılaştırır
     * 5. Başarılıysa Authentication nesnesi (isAuthenticated=true) döner
     * 6. Başarısızsa BadCredentialsException fırlatır
     *
     * @param request  Email ve şifre
     * @return Access + refresh token'ları içeren yanıt
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        // Spring Security authentication pipeline'ını tetikle
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // DB'den tam kullanıcı entity'sini al (Spring Security UserDetails'ten roles almak için)
        BaseUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Daha önce verilmiş refresh token'ları iptal et (tek oturum politikası)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Kullanıcı giriş yaptı: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Refresh token ile yeni access token üretir.
     *
     * @param request  Refresh token string'i
     * @return Yeni access + refresh token çifti
     */
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {

        // Refresh token'ı DB'de bul
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token bulunamadı"));

        // Token geçerliliğini kontrol et
        if (!refreshToken.isValid()) {
            throw new InvalidTokenException(refreshToken.isRevoked() ? "Token iptal edilmiş" : "Token süresi dolmuş");
        }

        // Kullanıcıyı yükle
        BaseUser user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(refreshToken.getUserId().toString()));

        // Eski refresh token'ı iptal et — token rotation güvenlik mekanizması
        // Her yenilemede yeni refresh token verilir, eski geçersiz kalır
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Token yenilendi: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Kullanıcı çıkış işlemi.
     * Tüm refresh token'ları iptal eder.
     *
     * @param userId  Çıkış yapan kullanıcının ID'si
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Kullanıcı çıkış yaptı: {}", userId);
        // Access token için blacklist mekanizması RedisConfig üzerinden yönetilir
        // Token JTI'si Redis'e yazılır, TTL = token kalan süresi
    }

    /**
     * Kullanıcı için access + refresh token üretir ve yanıt DTO'sunu hazırlar.
     * DRY prensibi: login ve register'da aynı kodun tekrarını önler.
     */
    private TokenResponse generateTokenResponse(BaseUser user) {

        // Rol adlarını string listesine çevir
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        String userId = user.getId().toString();

        // Access token: kısa ömürlü (15dk), her API isteğinde Authorization header'da gönderilir
        String accessToken = jwtUtil.generateAccessToken(userId, user.getEmail(), roles);

        // Refresh token: uzun ömürlü (7 gün), sadece token yenileme için kullanılır
        String refreshTokenStr = jwtUtil.generateRefreshToken(userId, user.getEmail(), roles);

        // Refresh token'ı DB'ye kaydet
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000) // milisaniye → saniye
                .roles(roles)
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .build();
    }
}
