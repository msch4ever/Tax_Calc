package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a vesting event from Restricted Stock CSV.
 * This is separate from sell transactions - vest income is taxable regardless of whether shares are sold.
 */
public record VestEvent(
        String grantNumber,
        LocalDate vestDate,
        int quantity,                    // Number of shares vested
        BigDecimal taxableGainUsd,       // Taxable gain from Tax Withholding row (USD)
        BigDecimal pricePerShare         // Calculated: taxableGainUsd / quantity
) {
    
    /**
     * Create a VestEvent with calculated price per share.
     */
    public static VestEvent of(String grantNumber, LocalDate vestDate, int quantity, BigDecimal taxableGainUsd) {
        BigDecimal pricePerShare = quantity > 0 
                ? taxableGainUsd.divide(BigDecimal.valueOf(quantity), 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new VestEvent(grantNumber, vestDate, quantity, taxableGainUsd, pricePerShare);
    }
    
    /**
     * Returns the year when shares vested.
     */
    public int vestYear() {
        return vestDate.getYear();
    }
}

