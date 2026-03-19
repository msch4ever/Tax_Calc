package com.klos.automation.taxable.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Exchange rate provider that combines daily rates from a delegate provider
 * with official GFŘ yearly average rates ("jednotný kurz").
 *
 * Per Czech tax law for capital gains:
 * - Sale proceeds: use yearly rate for the SALE year
 * - Cost basis: use yearly rate for the VEST/PURCHASE year
 *
 * This provider includes hardcoded historical yearly rates from 2019-2025.
 */
public class CombinedExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider dailyRateProvider;
    private final Map<Integer, BigDecimal> yearlyRates;
    private final int primaryTaxYear;

    /**
     * Official GFŘ yearly average rates (USD to CZK).
     * Source: https://financnisprava.gov.cz - "jednotné kurzy"
     */
    private static final Map<Integer, BigDecimal> HISTORICAL_YEARLY_RATES = Map.of(
            2019, new BigDecimal("22.93"),
            2020, new BigDecimal("23.14"),
            2021, new BigDecimal("21.72"),
            2022, new BigDecimal("23.41"),
            2023, new BigDecimal("22.14"),
            2024, new BigDecimal("23.28"),
            2025, new BigDecimal("21.84")
    );

    /**
     * Creates a combined provider with historical yearly rates.
     * Uses CNB for daily rates and hardcoded GFŘ rates for yearly averages.
     *
     * @param dailyRateProvider provider for daily rates (typically CnbExchangeRateProvider)
     */
    public CombinedExchangeRateProvider(ExchangeRateProvider dailyRateProvider) {
        this.dailyRateProvider = dailyRateProvider;
        this.yearlyRates = new HashMap<>(HISTORICAL_YEARLY_RATES);
        this.primaryTaxYear = LocalDate.now().getYear();
    }

    /**
     * Get the hardcoded yearly rate for a specific year.
     * Returns null if not available.
     */
    public static BigDecimal getHistoricalYearlyRate(int year) {
        return HISTORICAL_YEARLY_RATES.get(year);
    }

    @Override
    public BigDecimal getRate(LocalDate date) {
        return dailyRateProvider.getRate(date);
    }

    @Override
    public BigDecimal getYearlyAverageRate(int year) {
        BigDecimal rate = yearlyRates.get(year);
        if (rate == null) {
            throw new IllegalArgumentException("No yearly rate available for year " + year +
                ". Available years: 2019-2025");
        }
        return rate;
    }

    @Override
    public String getDescription() {
        BigDecimal primaryRate = yearlyRates.get(primaryTaxYear);
        return "Combined: Daily rates from " + dailyRateProvider.getDescription()
               + ", Yearly average: " + (primaryRate != null ? primaryRate : "N/A") + " CZK/USD (GFŘ)";
    }
}

