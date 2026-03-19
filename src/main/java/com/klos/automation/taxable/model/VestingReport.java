package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Report containing all vesting events for a tax year.
 * This represents the taxable income from RSU vesting (always taxable regardless of sale).
 */
public record VestingReport(
        int taxYear,
        List<TaxableVestEvent> vestEvents,
        
        // Totals
        int totalShares,
        BigDecimal totalTaxableGainUsd,
        BigDecimal totalTaxableGainCzkByDate,
        BigDecimal totalTaxableGainCzkByYearlyAvg
) {
    
    /**
     * Create a VestingReport from a list of taxable vest events.
     */
    public static VestingReport fromEvents(int taxYear, List<TaxableVestEvent> events) {
        int totalShares = events.stream()
                .mapToInt(TaxableVestEvent::quantity)
                .sum();
        
        BigDecimal totalUsd = events.stream()
                .map(TaxableVestEvent::taxableGainUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCzkByDate = events.stream()
                .map(TaxableVestEvent::taxableGainCzkByDate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCzkByYearlyAvg = events.stream()
                .map(TaxableVestEvent::taxableGainCzkByYearlyAvg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new VestingReport(taxYear, events, totalShares, totalUsd, totalCzkByDate, totalCzkByYearlyAvg);
    }

    /**
     * Create an empty report for when RSU is not selected.
     */
    public static VestingReport empty(int taxYear) {
        return new VestingReport(taxYear, List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

