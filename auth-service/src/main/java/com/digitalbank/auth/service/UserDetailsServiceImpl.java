package com.digitalbank.auth.service;

import com.digitalbank.auth.entity.BaseUser;
import com.digitalbank.auth.entity.Role;
import com.digitalbank.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Spring Security'nin kimlik doğrulama sürecinde kullandığı kullanıcı yükleme servisi.
 *
 * Spring Security akışı:
 * 1. AuthenticationManager.authenticate(token) çağrılır
 * 2. DaoAuthenticationProvider devreye girer
 * 3. loadUserByUsername() çağrılır → DB'den kullanıcı yüklenir
 * 4. Şifre BCrypt ile karşılaştırılır
 * 5. Başarılıysa Authentication nesnesi oluşturulur
 *
 * @RequiredArgsConstructor: final alanlar için constructor injection (Lombok)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Email'e göre kullanıcıyı veritabanından yükler.
     * Spring Security bu metodu authentication sırasında çağırır.
     *
     * @param email  Kullanıcı email adresi (Spring Security'de "username" olarak geçer)
     * @return UserDetails nesnesi — Spring Security'nin kullandığı kullanıcı temsili
     * @throws UsernameNotFoundException  Kullanıcı bulunamazsa fırlatılır
     */
    @Override
    @Transactional(readOnly = true)
    // readOnly = true: Bu metot yalnızca okuma yapar, veritabanı transaction'ı optimize edilir
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        BaseUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Kullanıcı bulunamadı: {}", email);
                    // Güvenlik: "kullanıcı yok" yerine genel mesaj — kullanıcı enum saldırısını önler
                    return new UsernameNotFoundException("Kullanıcı bulunamadı: " + email);
                });

        // Rolleri Spring Security GrantedAuthority formatına dönüştür
        // SimpleGrantedAuthority: rol adını authority olarak kabul eder
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        // Spring Security'nin User nesnesi — bizim entity değil
        // username: email, password: hashlenmiş şifre, authorities: roller
        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                // Hesap aktif değilse Spring Security otomatik reddeder
                .disabled(!user.isActive())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(!user.isActive())
                .build();
    }
}
