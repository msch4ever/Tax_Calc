package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Represents a processed transaction with tax calculations.
 * Contains both USD and CZK amounts with different conversion methods.
 *
 * Capital Gain/Loss calculation:
 * - USD: gainLossUsd = sellProceedsUsd - vestIncomeUsd (provided by E-Trade)
 * - CZK Daily: gainLossCzkByDate = (sellProceedsUsd × sellRate) - (vestIncomeUsd × vestRate)
 * - CZK Yearly: gainLossCzkByYearlyAvg = gainLossUsd × yearlyRate (same rate cancels out)
 */
public record TaxableTransaction(
        // Original transaction data
        StockTransaction original,

        // Holding period info
        Period holdingPeriod,
        boolean capitalGainsExempt,

        // USD amounts
        BigDecimal vestIncomeUsd,          // Ordinary Income Recognized (= Cost Basis for RSU)
        BigDecimal sellProceedsUsd,         // Total sale proceeds
        BigDecimal capitalGainLossUsd,      // Adjusted Gain/Loss (taxable only if not exempt)

        // Exchange rates used
        BigDecimal vestDateRate,            // USD/CZK rate on vest date
        BigDecimal sellDateRate,            // USD/CZK rate on sell date
        BigDecimal yearlyAvgRate,           // Yearly average rate

        // CZK amounts using transaction date rates
        BigDecimal vestIncomeCzkByDate,     // vestIncomeUsd × vestDateRate
        BigDecimal sellProceedsCzkByDate,   // sellProceedsUsd × sellDateRate
        BigDecimal capitalGainLossCzkByDate, // sellProceedsCzkByDate - vestIncomeCzkByDate

        // CZK amounts using yearly average rate
        BigDecimal vestIncomeCzkByYearlyAvg,
        BigDecimal capitalGainLossCzkByYearlyAvg  // capitalGainLossUsd × yearlyAvgRate
) {
    
    /**
     * Get the taxable capital gain/loss in USD.
     * Returns ZERO if exempt due to 3-year holding period.
     */
    public BigDecimal taxableCapitalGainUsd() {
        return capitalGainsExempt ? BigDecimal.ZERO : capitalGainLossUsd;
    }
    
    /**
     * Get the taxable capital gain/loss in CZK (using transaction date rate).
     * Returns ZERO if exempt due to 3-year holding period.
     */
    public BigDecimal taxableCapitalGainCzkByDate() {
        return capitalGainsExempt ? BigDecimal.ZERO : capitalGainLossCzkByDate;
    }
    
    /**
     * Get the taxable capital gain/loss in CZK (using yearly average rate).
     * Returns ZERO if exempt due to 3-year holding period.
     */
    public BigDecimal taxableCapitalGainCzkByYearlyAvg() {
        return capitalGainsExempt ? BigDecimal.ZERO : capitalGainLossCzkByYearlyAvg;
    }
    
    /**
     * Get total taxable amount in USD (vest income + taxable capital gain).
     */
    public BigDecimal totalTaxableUsd() {
        return vestIncomeUsd.add(taxableCapitalGainUsd());
    }
    
    /**
     * Get total taxable amount in CZK using transaction date rates.
     */
    public BigDecimal totalTaxableCzkByDate() {
        return vestIncomeCzkByDate.add(taxableCapitalGainCzkByDate());
    }
    
    /**
     * Get total taxable amount in CZK using yearly average rate.
     */
    public BigDecimal totalTaxableCzkByYearlyAvg() {
        return vestIncomeCzkByYearlyAvg.add(taxableCapitalGainCzkByYearlyAvg());
    }
    
    // Convenience accessors
    public String grantNumber() { return original.grantNumber(); }
    public LocalDate vestDate() { return original.vestDate() != null ? original.vestDate() : original.dateAcquired(); }
    public LocalDate sellDate() { return original.dateSold(); }
    public int quantity() { return original.quantity(); }
    public String symbol() { return original.symbol(); }

    // Price per share accessors
    /** FMV at vest date (RSU) or purchase date (ESPP) */
    public BigDecimal vestPricePerShare() { return original.vestDateFmv(); }

    /** Actual cost/acquisition price per share - for ESPP this is the discounted purchase price */
    public BigDecimal costPricePerShare() {
        return original.acquisitionCostPerShare() != null
            ? original.acquisitionCostPerShare()
            : original.vestDateFmv();
    }

    public BigDecimal sellPricePerShare() { return original.proceedsPerShare(); }
}

