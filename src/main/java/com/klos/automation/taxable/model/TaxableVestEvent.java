package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A vesting event with calculated CZK amounts.
 * Represents taxable income from vested shares.
 */
public record TaxableVestEvent(
        VestEvent original,
        
        // Exchange rates
        BigDecimal vestDateRate,
        BigDecimal yearlyAvgRate,
        
        // CZK amounts
        BigDecimal taxableGainCzkByDate,      // taxableGainUsd × vestDateRate
        BigDecimal taxableGainCzkByYearlyAvg  // taxableGainUsd × yearlyAvgRate
) {
    // Convenience accessors
    public String grantNumber() { return original.grantNumber(); }
    public LocalDate vestDate() { return original.vestDate(); }
    public int quantity() { return original.quantity(); }
    public BigDecimal taxableGainUsd() { return original.taxableGainUsd(); }
    public BigDecimal pricePerShare() { return original.pricePerShare(); }
}

