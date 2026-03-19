package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An ESPP purchase event with calculated CZK amounts.
 * Represents taxable income from the employer discount benefit.
 */
public record TaxableEsppPurchase(
        EsppPurchaseEvent original,
        
        // Exchange rates
        BigDecimal purchaseDateRate,
        BigDecimal yearlyAvgRate,
        
        // CZK amounts
        BigDecimal discountBenefitCzkByDate,      // discountBenefitUsd × purchaseDateRate
        BigDecimal discountBenefitCzkByYearlyAvg  // discountBenefitUsd × yearlyAvgRate
) {
    // Convenience accessors
    public String grantNumber() { return original.grantNumber(); }
    public LocalDate purchaseDate() { return original.purchaseDate(); }
    public int quantity() { return original.quantity(); }
    public BigDecimal purchasePrice() { return original.purchasePrice(); }
    public BigDecimal purchaseDateFmv() { return original.purchaseDateFmv(); }
    public BigDecimal discountBenefitUsd() { return original.discountBenefitUsd(); }
    public BigDecimal discountPerShare() { return original.discountPerShare(); }
}

