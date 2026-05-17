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
 * KİMLİK DOĞRULAMA SERVİSİ — Bankacılık Güvenlik Katmanı
 * =========================================================
 *
 * Bankacılık uygulaması standart web uygulamasından farklı güvenlik gerektirir:
 *
 * 1. TC Kimlik No doğrulaması (KYC — Know Your Customer):
 *    Türk bankacılık mevzuatı: her müşteri gerçek kimlikle doğrulanmalı.
 *    Sahte TC No ile hesap açılması kara para aklama (AML) ihlalidir.
 *    Algoritma kontrolü: 11 haneli TC No'nun son 2 hanesi matematiksel türetilir.
 *    Sadece uzunluk kontrolü yetmez — sahte ama uzunluğu doğru TC'ler vardır.
 *
 * 2. Tek oturum politikası (Single Session):
 *    Her login yeni refresh token → eski token iptal edilir.
 *    Kullanıcı aynı anda yalnızca bir aktif oturuma sahip olabilir.
 *    Güvenlik: Çalınmış refresh token login anında devre dışı kalır.
 *
 * 3. Token rotation (Döndürme):
 *    refreshToken() her çağrıda eski token iptal, yeni token verir.
 *    Çalınmış refresh token bir kez kullanılırsa sonraki kullanım reddedilir.
 *
 * 4. Stateless access token:
 *    JWT access token (15 dk) blacklist'e alınamaz — stateless olduğu için.
 *    logout() sadece refresh token'ı iptal eder.
 *    Access token süresi bittiğinde (15 dk) doğal olarak geçersizleşir.
 *    Kabul edilebilir risk: 15 dk içinde saldırgan token'ı kullanabilir.
 *    Alternatif: Redis blacklist — her API isteğinde Redis sorgusu (performance maliyeti).
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
     * @Transactional neden gerekli?
     *   save() sonrası rol ataması başarısız olursa kayıt geri alınmalı.
     *   Yarım kalmış müşteri kaydı (rol atanamadı) tutarsız veridir.
     *   @Transactional: tüm adımlar tek atomik işlem — hepsi başarılı ya da hiçbiri.
     *
     * Kayıt sırası neden önemli?
     *   1. TC doğrulaması: En önce — gereksiz DB sorgusu önlenir.
     *   2. Email duplicate check: DB sorgusu — önce hafif validasyon.
     *   3. TC duplicate check: DB sorgusu — son kontrol.
     *   4. Kayıt + rol + token: Tüm validasyonlar geçtikten sonra.
     *   Erken fail (early return): Geçersiz girdilerle DB'ye gitmemek.
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {

        // TC kimlik no algoritma doğrulaması — sahte TC ile kayıt önlenir
        // KYC (Know Your Customer): Bankacılık mevzuatı gereği zorunlu
        // Algoritma: TC No'nun 10. hanesi = (1..9. hanelerin ağırlıklı toplamı) mod 10
        // Sadece uzunluk veya format kontrolü yetmez — sahte TC'ler bu testi geçemez
        if (!ValidationUtils.isValidTcNo(request.getTcNo())) {
            throw new IllegalArgumentException("Geçersiz TC Kimlik Numarası");
        }

        // Email tekrar eden kayıt kontrolü
        // Neden ayrı mesaj (email vs TC)?
        //   "Bu bilgi ile hesap var" gibi genel mesaj güvenlik açığı yaratır
        //   Kullanıcı hangi bilgisinin zaten kayıtlı olduğunu bilmeli (UX)
        //   Farklı hata mesajı: çağıran kod doğru yönlendirme yapabilir
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu email adresi zaten kayıtlı");
        }

        // TC No tekrar eden kayıt kontrolü
        // Aynı kişi iki hesap açamasın (bankacılık: tek kişi, tek hesap politikası)
        if (userRepository.existsByTcNo(request.getTcNo())) {
            throw new IllegalArgumentException("Bu TC Kimlik No ile zaten hesap açılmış");
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());

        // BCrypt ile şifre hashleme — neden asla plain text saklanmaz?
        //   DB sızıntısı durumunda: hashlenmiş şifre kırılmak için brute force gerekir.
        //   BCrypt: her hash farklı salt içerir → aynı şifre farklı hash üretir.
        //   Tek yönlü: hash'ten şifre geri üretilemez, sadece doğrulama yapılabilir.
        //   BCrypt.matches(plainText, hash) → login doğrulamasında kullanılır.
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer.setTcNo(request.getTcNo());
        customer.setPhone(request.getPhone());
        customer.setMonthlyIncome(request.getMonthlyIncome());

        // customerNo formatı: "DB" + UUID'den 8 karakter (büyük harf)
        //   "DB" prefix: DigitalBank markası — hangi bankaya ait olduğu belli
        //   UUID substring: Çakışma ihtimali astronomik düşük (2^32 kombinasyon)
        //   Alternatif: DB sequence (1, 2, 3...) → tahmin edilebilir, enumeration saldırısına açık
        //   DB<UUID8> format: "DB3A7F2C19" gibi — müşteriye bildirilir, şube işlemlerinde kullanılır
        customer.setCustomerNo("DB" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());

        // Varsayılan rol ata: ROLE_CUSTOMER
        // Neden DB'den alınır, sabit kod yazılmaz?
        //   Role entity ilişkisi (ManyToMany) JPA tarafından yönetilmeli — managed entity olmalı.
        //   "ROLE_CUSTOMER" string sabit kod yazmak: rol tablosundan kopar.
        //   orElseThrow: Rol seed data eksikse açık hata — sessizce devam etmez.
        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER bulunamadı — seed data eksik"));
        customer.getRoles().add(customerRole);

        Customer savedCustomer = (Customer) userRepository.save(customer);
        log.info("Yeni müşteri kaydedildi: {} ({})", savedCustomer.getEmail(), savedCustomer.getId());

        // Kayıt sonrası otomatik giriş: kullanıcı tekrar login ekranına yönlendirilmez
        return generateTokenResponse(savedCustomer);
    }

    /**
     * Kullanıcı giriş işlemi.
     *
     * Spring Security AuthenticationManager tam akış:
     *   1. UsernamePasswordAuthenticationToken oluşturulur (credentials taşır, henüz doğrulanmamış)
     *   2. authenticationManager.authenticate() → ProviderManager'a devredilir
     *   3. ProviderManager → DaoAuthenticationProvider'ı dener
     *   4. DaoAuthenticationProvider.loadUserByUsername(email) → UserDetailsService çağrılır
     *   5. DB'den kullanıcı bulunur → UserDetails nesnesine map edilir
     *   6. passwordEncoder.matches(request.password, storedHash) → BCrypt karşılaştırma
     *   7. Başarılıysa: isAuthenticated=true olan Authentication nesnesi döner
     *   8. Başarısızsa: BadCredentialsException → 401 Unauthorized
     *
     * Neden authenticate() sonra tekrar DB sorgusu?
     *   Authentication nesnesi UserDetails döner — roller için tam entity gerekebilir.
     *   Özellikle email değişmişse (nadir) ya da ek alanlar (customerId) lazımsa DB gerekir.
     *
     * Neden login'de de revokeAllByUserId çağrılır?
     *   Tek oturum politikası: Yeni login → tüm eski oturumlar geçersizleşir.
     *   Kullanıcı farklı cihazdan giriş yaparsa eski cihazın oturumu kapanır.
     *   Güvenlik: Çalınmış refresh token, kullanıcı tekrar login olunca geçersizleşir.
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

        // Tek oturum politikası: Yeni login → önceki tüm refresh token'lar iptal edilir
        // Kullanıcı aynı anda yalnızca bir aktif oturumda olabilir
        // Güvenlik senaryosu: Eski token çalındıysa yeni login onu geçersiz kılar
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Kullanıcı giriş yaptı: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Refresh token ile yeni token çifti üretir — Token Rotation Pattern.
     *
     * Token Rotation neden önemlidir?
     *   Senaryo: Refresh token çalındı.
     *   Rotation olmadan: Saldırgan refresh token'ı süresiz kullanabilir (7 gün).
     *   Rotation ile:
     *     - Meşru kullanıcı token'ı yeniler → eski token iptal.
     *     - Saldırgan eski (artık iptal edilmiş) token'ı kullanmaya çalışır → HATA.
     *     - Saldırgan önce yenilerse: meşru kullanıcı yenilemeye çalışır → iptal token → HATA.
     *     - Her iki durumda da zincir kırılır, saldırı tespit edilebilir.
     *
     * Rotation akışı:
     *   1. Gelen token DB'de var mı, geçerli mi? Kontrol et.
     *   2. Eski token'ı revoked=true yap → artık kullanılamaz.
     *   3. Yeni token çifti üret ve DB'ye kaydet.
     *   4. Yeni tokenları döndür.
     */
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {

        // Refresh token'ı DB'de bul — JWT imzası değil, DB kaydı geçerlilik kriteridir
        // Neden DB'de? Access token'ın aksine refresh token iptal edilebilir olmalı (stateful)
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token bulunamadı"));

        // Token geçerliliğini kontrol et (revoked veya expired)
        // isValid(): revoked=false AND expiryDate.isAfter(now)
        if (!refreshToken.isValid()) {
            throw new InvalidTokenException(refreshToken.isRevoked() ? "Token iptal edilmiş" : "Token süresi dolmuş");
        }

        BaseUser user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(refreshToken.getUserId().toString()));

        // Token Rotation: Kullanılan eski token derhal iptal edilir
        // Bir refresh token yalnızca BİR KEZ kullanılabilir — sonraki yenileme yeni token ister
        // Çalınmış token kullanılırsa sonraki kullanım reddedilir → güvenlik uyarısı
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Token yenilendi: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Kullanıcı çıkış işlemi.
     *
     * Neden sadece refresh token iptal edilir, access token değil?
     *   Access token: JWT → stateless → sunucuda tutulmaz → iptal edilemez.
     *   Çözüm seçenekleri:
     *     A) Kısa ömürlü access token (15 dk) → logout sonrası en fazla 15 dk geçerli.
     *        Kabul edilebilir risk: bankacılık için genellikle yeterli.
     *     B) Redis blacklist: Her logout'ta token JTI'si Redis'e yazılır, TTL = token süresi.
     *        Her API isteğinde Redis kontrolü → performans maliyeti.
     *        Bu uygulamada: Yorum satırında belirtildiği gibi Redis seçeneği de mevcut.
     *
     * revokeAllByUserId: Kullanıcının TÜM cihazlarındaki refresh token'larını iptal eder.
     *   Senaryo: "Tüm cihazlardan çıkış yap" özelliği için de aynı yöntem kullanılır.
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Kullanıcı çıkış yaptı: {}", userId);
        // Access token blacklist için Redis kullanılabilir:
        // redisTemplate.opsForValue().set("blacklist:" + jti, "revoked", remainingTtl, SECONDS)
        // Her /api/** isteğinde JwtFilter: Redis'te JTI var mı kontrol eder
    }

    /**
     * Access + refresh token çifti üretir — DRY (Don't Repeat Yourself) Prensibi.
     *
     * Neden ayrı private metod?
     *   login(), register() ve refreshToken() hepsi aynı token üretme mantığını kullanır.
     *   Tek bir metodda toplamak: kod tekrarını önler, değişiklik tek yerde yapılır.
     *   Örnek: Token süresini değiştirmek → 3 yerde değil, 1 yerde değişir.
     *
     * Access token özellikleri:
     *   - Kısa ömürlü (15 dk): Her API isteğinde Authorization: Bearer <token> header'ında
     *   - İçerir: userId, email, roller (JWT claim'leri)
     *   - Stateless: Sunucuda tutulmaz, imza doğrulaması yeterli
     *
     * Refresh token özellikleri:
     *   - Uzun ömürlü (7 gün): Sadece /auth/refresh endpoint'ine gönderilir
     *   - DB'de saklanır: İptal edilebilir (logout, şüpheli aktivite)
     *   - Her kullanımda yeni token verilir (rotation)
     */
    private TokenResponse generateTokenResponse(BaseUser user) {

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        String userId = user.getId().toString();

        // Access token: kısa ömürlü (15 dk), her API isteğinde header'da gönderilir
        String accessToken = jwtUtil.generateAccessToken(userId, user.getEmail(), roles);

        // Refresh token: uzun ömürlü (7 gün), sadece yeni access token almak için kullanılır
        String refreshTokenStr = jwtUtil.generateRefreshToken(userId, user.getEmail(), roles);

        // Refresh token DB'ye kaydedilir (access token kaydedilmez — stateless)
        // expiryDate: milisaniye → saniye dönüşümü (jwtUtil millisaniye cinsinden döner)
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
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000) // milisaniye → saniye (frontend için)
                .roles(roles)
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .build();
    }
}
