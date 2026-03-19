package com.klos.automation.taxable.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstraction for providing USD to CZK exchange rates.
 * Implementations can provide rates from various sources:
 * - Manual input
 * - CNB (Czech National Bank) API
 * - Other sources
 */
public interface ExchangeRateProvider {
    
    /**
     * Get the USD to CZK exchange rate for a specific date.
     *
     * @param date the date for which to get the rate
     * @return the exchange rate (1 USD = X CZK)
     */
    BigDecimal getRate(LocalDate date);
    
    /**
     * Get the average USD to CZK exchange rate for an entire year.
     * This is typically the CNB official yearly average rate.
     *
     * @param year the year for which to get the average rate
     * @return the average exchange rate for the year
     */
    BigDecimal getYearlyAverageRate(int year);
    
    /**
     * Convert an amount from USD to CZK using the rate for a specific date.
     *
     * @param usdAmount the amount in USD
     * @param date      the date for which to use the exchange rate
     * @return the amount in CZK
     */
    default BigDecimal convertToCzk(BigDecimal usdAmount, LocalDate date) {
        return usdAmount.multiply(getRate(date));
    }
    
    /**
     * Convert an amount from USD to CZK using the yearly average rate.
     *
     * @param usdAmount the amount in USD
     * @param year      the year for which to use the average rate
     * @return the amount in CZK
     */
    default BigDecimal convertToCzkYearlyAverage(BigDecimal usdAmount, int year) {
        return usdAmount.multiply(getYearlyAverageRate(year));
    }
    
    /**
     * Get a description of this rate provider (for display purposes).
     *
     * @return description of the rate source
     */
    String getDescription();
}

