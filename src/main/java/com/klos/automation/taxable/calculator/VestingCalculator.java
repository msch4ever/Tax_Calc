package com.klos.automation.taxable.calculator;

import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.model.TaxableVestEvent;
import com.klos.automation.taxable.model.VestEvent;
import com.klos.automation.taxable.model.VestingReport;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Calculator for vesting income.
 * Converts vest events to CZK and calculates totals.
 */
public class VestingCalculator {
    
    private static final Logger LOG = Logger.getLogger(VestingCalculator.class.getName());
    
    private final ExchangeRateProvider exchangeRateProvider;
    
    public VestingCalculator(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }
    
    /**
     * Calculate taxable vesting income for a specific year.
     * Results are sorted by vest date first, then by grant number.
     */
    public VestingReport calculateForYear(List<VestEvent> vestEvents, int taxYear) {
        List<TaxableVestEvent> taxableEvents = vestEvents.stream()
                .filter(e -> e.vestYear() == taxYear)
                .sorted((a, b) -> {
                    int dateCompare = a.vestDate().compareTo(b.vestDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.grantNumber().compareTo(b.grantNumber());
                })
                .map(e -> processVestEvent(e, taxYear))
                .toList();

        return VestingReport.fromEvents(taxYear, taxableEvents);
    }
    
    private TaxableVestEvent processVestEvent(VestEvent event, int taxYear) {
        BigDecimal vestDateRate = exchangeRateProvider.getRate(event.vestDate());
        BigDecimal yearlyAvgRate = exchangeRateProvider.getYearlyAverageRate(taxYear);
        
        BigDecimal czkByDate = event.taxableGainUsd().multiply(vestDateRate);
        BigDecimal czkByYearlyAvg = event.taxableGainUsd().multiply(yearlyAvgRate);
        
        logVestEvent(event, vestDateRate, czkByDate, yearlyAvgRate, czkByYearlyAvg);
        
        return new TaxableVestEvent(event, vestDateRate, yearlyAvgRate, czkByDate, czkByYearlyAvg);
    }
    
    private void logVestEvent(VestEvent event, BigDecimal rate, BigDecimal czkByDate,
                               BigDecimal yearlyRate, BigDecimal czkByYearly) {
        LOG.info(() -> String.format(
                "─── VEST Grant %s | %s ───",
                event.grantNumber(), event.vestDate()
        ));
        LOG.info(() -> String.format(
                "  %d shares × $%.2f = $%,.2f | rate %.3f → %,.2f CZK | yearly @%.3f → %,.2f CZK",
                event.quantity(),
                event.pricePerShare().doubleValue(),
                event.taxableGainUsd().doubleValue(),
                rate.doubleValue(),
                czkByDate.doubleValue(),
                yearlyRate.doubleValue(),
                czkByYearly.doubleValue()
        ));
    }
}

