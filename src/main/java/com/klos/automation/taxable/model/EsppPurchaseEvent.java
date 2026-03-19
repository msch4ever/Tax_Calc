package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an ESPP purchase event.
 * The taxable gain is the discount benefit: (Purchase Date FMV - Purchase Price) × quantity
 */
public record EsppPurchaseEvent(
        String grantNumber,
        LocalDate purchaseDate,
        int quantity,                      // Number of shares purchased
        BigDecimal purchasePrice,          // Price paid (already includes 15% discount)
        BigDecimal purchaseDateFmv,        // Fair Market Value at purchase date
        BigDecimal discountBenefitUsd      // Calculated: (FMV - Purchase Price) × quantity
) {
    
    /**
     * Create an EsppPurchaseEvent with calculated discount benefit.
     */
    public static EsppPurchaseEvent of(String grantNumber, LocalDate purchaseDate, int quantity,
                                        BigDecimal purchasePrice, BigDecimal purchaseDateFmv) {
        BigDecimal discountPerShare = purchaseDateFmv.subtract(purchasePrice);
        BigDecimal discountBenefitUsd = discountPerShare.multiply(BigDecimal.valueOf(quantity));
        return new EsppPurchaseEvent(grantNumber, purchaseDate, quantity, purchasePrice, purchaseDateFmv, discountBenefitUsd);
    }
    
    /**
     * Returns the year when shares were purchased.
     */
    public int purchaseYear() {
        return purchaseDate.getYear();
    }
    
    /**
     * Returns the discount per share.
     */
    public BigDecimal discountPerShare() {
        return purchaseDateFmv.subtract(purchasePrice);
    }
}

