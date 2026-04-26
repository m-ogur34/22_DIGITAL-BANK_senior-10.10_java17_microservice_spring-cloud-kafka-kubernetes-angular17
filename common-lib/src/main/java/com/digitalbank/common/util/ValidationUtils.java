package com.digitalbank.common.util;

/**
 * TC Kimlik Numarası ve diğer doğrulama yardımcıları.
 *
 * TC Kimlik No algoritması (Nüfus ve Vatandaşlık İşleri Genel Müdürlüğü):
 * - 11 haneli sayı
 * - İlk hane 0 olamaz
 * - 10. hane: (1,3,5,7,9. hanelerin toplamı * 7 - 2,4,6,8. hanelerin toplamı) mod 10
 * - 11. hane: ilk 10 hanenin toplamı mod 10
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * TC Kimlik Numarasının geçerli olup olmadığını doğrular.
     *
     * @param tcNo  11 haneli TC kimlik numarası string'i
     * @return Geçerliyse true
     */
    public static boolean isValidTcNo(String tcNo) {
        // Null ve uzunluk kontrolü
        if (tcNo == null || tcNo.length() != 11) return false;

        // Yalnızca rakamlardan oluşmalı
        if (!tcNo.matches("\\d{11}")) return false;

        // İlk hane 0 olamaz
        if (tcNo.charAt(0) == '0') return false;

        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = Character.getNumericValue(tcNo.charAt(i));
        }

        // Tek indeksli hanelerin toplamı (1., 3., 5., 7., 9. hane — 0-indexed: 0,2,4,6,8)
        int oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];

        // Çift indeksli hanelerin toplamı (2., 4., 6., 8. hane — 0-indexed: 1,3,5,7)
        int evenSum = digits[1] + digits[3] + digits[5] + digits[7];

        // 10. hane doğrulama (0-indexed: 9)
        int tenthDigit = (oddSum * 7 - evenSum) % 10;
        if (tenthDigit < 0) tenthDigit += 10;
        if (tenthDigit != digits[9]) return false;

        // 11. hane doğrulama (0-indexed: 10)
        int sumFirstTen = 0;
        for (int i = 0; i < 10; i++) sumFirstTen += digits[i];
        int eleventhDigit = sumFirstTen % 10;

        return eleventhDigit == digits[10];
    }

    /**
     * Email formatını basit regex ile doğrular.
     * Kapsamlı doğrulama için Bean Validation @Email kullanılmalıdır.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Şifrenin güçlülük kriterlerini karşılayıp karşılamadığını kontrol eder.
     * Kriter: en az 8 karakter, 1 büyük harf, 1 küçük harf, 1 rakam.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}
