package com.klos.automation.taxable.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Manual exchange rate provider where rates are explicitly set.
 * Useful for testing and when API access is not available.
 */
public class ManualExchangeRateProvider implements ExchangeRateProvider {
    
    private final Map<LocalDate, BigDecimal> dateRates = new HashMap<>();
    private final Map<Integer, BigDecimal> yearlyAverageRates = new HashMap<>();
    private BigDecimal defaultRate = BigDecimal.ZERO;
    
    /**
     * Set the exchange rate for a specific date.
     *
     * @param date the date
     * @param rate the USD to CZK rate
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setRate(LocalDate date, BigDecimal rate) {
        dateRates.put(date, rate);
        return this;
    }
    
    /**
     * Set the exchange rate for a specific date.
     *
     * @param date the date
     * @param rate the USD to CZK rate as a string
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setRate(LocalDate date, String rate) {
        return setRate(date, new BigDecimal(rate));
    }
    
    /**
     * Set the yearly average exchange rate.
     *
     * @param year the year
     * @param rate the average USD to CZK rate
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setYearlyAverageRate(int year, BigDecimal rate) {
        yearlyAverageRates.put(year, rate);
        return this;
    }
    
    /**
     * Set the yearly average exchange rate.
     *
     * @param year the year
     * @param rate the average USD to CZK rate as a string
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setYearlyAverageRate(int year, String rate) {
        return setYearlyAverageRate(year, new BigDecimal(rate));
    }
    
    /**
     * Set a default rate to use when no specific rate is found.
     *
     * @param rate the default rate
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setDefaultRate(BigDecimal rate) {
        this.defaultRate = rate;
        return this;
    }
    
    /**
     * Set a default rate to use when no specific rate is found.
     *
     * @param rate the default rate as a string
     * @return this provider for chaining
     */
    public ManualExchangeRateProvider setDefaultRate(String rate) {
        return setDefaultRate(new BigDecimal(rate));
    }
    
    @Override
    public BigDecimal getRate(LocalDate date) {
        return dateRates.getOrDefault(date, defaultRate);
    }
    
    @Override
    public BigDecimal getYearlyAverageRate(int year) {
        return yearlyAverageRates.getOrDefault(year, defaultRate);
    }
    
    @Override
    public String getDescription() {
        return "Manual Exchange Rate Provider";
    }
}

