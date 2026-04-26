package com.digitalbank.common.util;

import java.math.BigInteger;
import java.util.Random;

/**
 * Türk IBAN numarası üretici.
 *
 * IBAN yapısı (TR için): TR + 2 kontrol hanesi + 5 banka kodu + 1 şube + 16 hesap no
 * Toplam: 26 karakter
 *
 * ISO 13616 standardına göre IBAN doğrulama:
 * 1. IBAN'ın ilk 4 karakterini sona taşı
 * 2. Harfleri sayıya çevir (A=10, B=11, ..., Z=35)
 * 3. Oluşan büyük sayıyı 97'ye böl
 * 4. Kalan 1 ise IBAN geçerlidir
 */
public final class IbanGenerator {

    // Türkiye ülke kodu
    private static final String COUNTRY_CODE = "TR";

    // Demo bankamızın kodu (gerçek sistemde merkez bankasından alınır)
    private static final String BANK_CODE = "00061";

    // Şube kodu (simülasyon)
    private static final String BRANCH_CODE = "0";

    private IbanGenerator() {}

    /**
     * Yeni bir Türk IBAN numarası üretir.
     * Her çağrıda benzersiz bir hesap numarası üretilir.
     *
     * @return "TR" ile başlayan 26 karakterlik IBAN string'i
     */
    public static String generate() {
        // 16 haneli rastgele hesap numarası üret
        String accountNumber = generateAccountNumber();

        // BBAN (Basic Bank Account Number): banka + şube + hesap
        String bban = BANK_CODE + BRANCH_CODE + accountNumber;

        // Kontrol hanelerini hesapla
        String checkDigits = calculateCheckDigits(COUNTRY_CODE, bban);

        return COUNTRY_CODE + checkDigits + bban;
    }

    /**
     * 16 haneli benzersiz hesap numarası üretir.
     * Gerçek sistemde veritabanından sequence ile alınır.
     */
    private static String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * ISO 13616'ya göre IBAN kontrol hanelerini hesaplar.
     *
     * @param countryCode  Ülke kodu (TR)
     * @param bban         Temel banka hesap numarası
     * @return 2 haneli kontrol basamağı string'i
     */
    private static String calculateCheckDigits(String countryCode, String bban) {
        // Geçici kontrol haneleri "00" ile string oluştur
        String ibanWithZeroCheck = bban + countryCode + "00";

        // Harfleri sayıya çevir: T=29, R=27
        StringBuilder numericIban = new StringBuilder();
        for (char c : ibanWithZeroCheck.toCharArray()) {
            if (Character.isLetter(c)) {
                // A=10, B=11 ... Z=35
                numericIban.append(Character.getNumericValue(c));
            } else {
                numericIban.append(c);
            }
        }

        // 97'ye böldükten sonra 98'den çıkar → kontrol hanesi
        BigInteger ibanNum = new BigInteger(numericIban.toString());
        int remainder = ibanNum.mod(BigInteger.valueOf(97)).intValue();
        int checkDigits = 98 - remainder;

        // 2 haneli string'e çevir (5 → "05")
        return String.format("%02d", checkDigits);
    }

    /**
     * Verilen IBAN'ın geçerli olup olmadığını doğrular.
     *
     * @param iban  Doğrulanacak IBAN string'i
     * @return Geçerliyse true
     */
    public static boolean validate(String iban) {
        if (iban == null || iban.length() != 26) return false;
        if (!iban.startsWith("TR")) return false;

        // İlk 4 karakteri sona taşı
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Harfleri sayıya çevir
        StringBuilder numericIban = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numericIban.append(Character.getNumericValue(c));
            } else {
                numericIban.append(c);
            }
        }

        // 97'ye bölümün kalanı 1 ise geçerli
        BigInteger ibanNum = new BigInteger(numericIban.toString());
        return ibanNum.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
