package com.klos.automation.taxable.calculator;

import com.klos.automation.taxable.model.StockTransaction;

import java.time.LocalDate;
import java.time.Period;

/**
 * Calculates holding period and determines tax exemption based on Czech tax law.
 * 
 * Czech tax law:
 * - Income at vest (Ordinary Income Recognized) is ALWAYS taxable
 * - Capital gains at sale are taxable only if shares held for 3 years or less
 * - If held more than 3 years, capital gains are exempt
 */
public class HoldingPeriodCalculator {
    
    private static final int EXEMPTION_YEARS = 3;
    
    /**
     * Calculate the holding period between vest date and sell date.
     *
     * @param vestDate the date shares were vested/acquired
     * @param sellDate the date shares were sold
     * @return the period between vest and sell
     */
    public Period calculateHoldingPeriod(LocalDate vestDate, LocalDate sellDate) {
        if (vestDate == null || sellDate == null) {
            return Period.ZERO;
        }
        return Period.between(vestDate, sellDate);
    }
    
    /**
     * Calculate the holding period for a transaction.
     *
     * @param transaction the stock transaction
     * @return the period between vest and sell
     */
    public Period calculateHoldingPeriod(StockTransaction transaction) {
        LocalDate vestDate = transaction.vestDate() != null 
                ? transaction.vestDate() 
                : transaction.dateAcquired();
        return calculateHoldingPeriod(vestDate, transaction.dateSold());
    }
    
    /**
     * Check if the capital gains are exempt from taxation due to holding period.
     * Capital gains are exempt if shares were held for MORE than 3 years.
     *
     * @param vestDate the date shares were vested/acquired
     * @param sellDate the date shares were sold
     * @return true if capital gains are exempt (held > 3 years)
     */
    public boolean isCapitalGainsExempt(LocalDate vestDate, LocalDate sellDate) {
        Period holdingPeriod = calculateHoldingPeriod(vestDate, sellDate);
        return holdingPeriod.getYears() > EXEMPTION_YEARS 
               || (holdingPeriod.getYears() == EXEMPTION_YEARS 
                   && (holdingPeriod.getMonths() > 0 || holdingPeriod.getDays() > 0));
    }
    
    /**
     * Check if capital gains are exempt for a transaction.
     *
     * @param transaction the stock transaction
     * @return true if capital gains are exempt (held > 3 years)
     */
    public boolean isCapitalGainsExempt(StockTransaction transaction) {
        LocalDate vestDate = transaction.vestDate() != null 
                ? transaction.vestDate() 
                : transaction.dateAcquired();
        return isCapitalGainsExempt(vestDate, transaction.dateSold());
    }
    
    /**
     * Format holding period for display.
     *
     * @param period the holding period
     * @return formatted string like "2y 3m 15d"
     */
    public String formatHoldingPeriod(Period period) {
        StringBuilder sb = new StringBuilder();
        if (period.getYears() > 0) {
            sb.append(period.getYears()).append("y ");
        }
        if (period.getMonths() > 0) {
            sb.append(period.getMonths()).append("m ");
        }
        sb.append(period.getDays()).append("d");
        return sb.toString().trim();
    }
}

