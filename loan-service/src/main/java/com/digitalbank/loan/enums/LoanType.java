package com.digitalbank.loan.enums;

/**
 * Kredi türleri — her türün farklı faiz oranı ve maksimum tutarı var.
 */
public enum LoanType {

    /** İhtiyaç kredisi: Genel amaçlı. Düşük tutar, yüksek faiz. Maks: 500.000 TL */
    IHTIYAC("İhtiyaç Kredisi", java.math.BigDecimal.valueOf(500_000), java.math.BigDecimal.valueOf(2.50)),

    /** Konut kredisi (mortgage): Uzun vade, düşük faiz. Maks: 10.000.000 TL */
    KONUT("Konut Kredisi", java.math.BigDecimal.valueOf(10_000_000), java.math.BigDecimal.valueOf(1.20)),

    /** Taşıt kredisi: Araç alımı için. Orta faiz. Maks: 2.000.000 TL */
    TASIT("Taşıt Kredisi", java.math.BigDecimal.valueOf(2_000_000), java.math.BigDecimal.valueOf(1.80));

    private final String displayName;
    private final java.math.BigDecimal maxAmount;        // Maksimum kredi tutarı (TL)
    private final java.math.BigDecimal monthlyInterestRate; // Aylık faiz oranı (%)

    LoanType(String displayName, java.math.BigDecimal maxAmount, java.math.BigDecimal monthlyInterestRate) {
        this.displayName = displayName;
        this.maxAmount = maxAmount;
        this.monthlyInterestRate = monthlyInterestRate;
    }

    public String getDisplayName() { return displayName; }
    public java.math.BigDecimal getMaxAmount() { return maxAmount; }
    public java.math.BigDecimal getMonthlyInterestRate() { return monthlyInterestRate; }
}
