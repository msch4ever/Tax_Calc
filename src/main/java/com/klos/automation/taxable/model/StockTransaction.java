package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single stock transaction from E-Trade Gains & Losses CSV.
 * Immutable record containing all relevant data for tax calculations.
 */
public record StockTransaction(
        String recordType,          // "Sell", "Summary"
        String symbol,              // e.g., "PSTG"
        PlanType planType,          // RS (RSU) or ESPP
        int quantity,
        LocalDate dateAcquired,     // Same as vest date for RSU
        LocalDate vestDate,
        LocalDate dateSold,
        BigDecimal ordinaryIncomeRecognized,  // Taxable at vest (always) = Cost Basis for RSU
        BigDecimal totalProceeds,              // Total sale proceeds
        BigDecimal gainLoss,                   // Capital gain/loss at sale (USD)
        BigDecimal proceedsPerShare,           // Sale price per share
        BigDecimal vestDateFmv,                // Fair market value at vest/purchase date
        BigDecimal acquisitionCostPerShare,    // Actual purchase price per share (for ESPP)
        String grantNumber
) {
    
    /**
     * Checks if this transaction is an RSU (Restricted Stock Unit).
     */
    public boolean isRsu() {
        return planType == PlanType.RS;
    }
    
    /**
     * Checks if this transaction is an ESPP (Employee Stock Purchase Plan).
     */
    public boolean isEspp() {
        return planType == PlanType.ESPP;
    }
    
    /**
     * Returns the year when the sale occurred.
     */
    public int sellYear() {
        return dateSold.getYear();
    }
    
    /**
     * Returns the year when shares vested.
     */
    public int vestYear() {
        return vestDate != null ? vestDate.getYear() : dateAcquired.getYear();
    }
}

