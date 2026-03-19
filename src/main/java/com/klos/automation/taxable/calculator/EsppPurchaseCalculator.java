package com.klos.automation.taxable.calculator;

import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.model.EsppPurchaseEvent;
import com.klos.automation.taxable.model.EsppPurchaseReport;
import com.klos.automation.taxable.model.TaxableEsppPurchase;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Calculator for ESPP purchase discount benefit income.
 * Converts ESPP purchase events to CZK and calculates totals.
 * 
 * ESPP taxable income = (Purchase Date FMV - Purchase Price) × Quantity
 * This represents the employer-provided discount, which is taxable at purchase.
 */
public class EsppPurchaseCalculator {
    
    private static final Logger LOG = Logger.getLogger(EsppPurchaseCalculator.class.getName());
    
    private final ExchangeRateProvider exchangeRateProvider;
    
    public EsppPurchaseCalculator(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }
    
    /**
     * Calculate taxable ESPP purchase income for a specific year.
     * Results are sorted by purchase date first, then by grant number.
     */
    public EsppPurchaseReport calculateForYear(List<EsppPurchaseEvent> purchases, int taxYear) {
        List<TaxableEsppPurchase> taxablePurchases = purchases.stream()
                .filter(e -> e.purchaseYear() == taxYear)
                .sorted((a, b) -> {
                    int dateCompare = a.purchaseDate().compareTo(b.purchaseDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.grantNumber().compareTo(b.grantNumber());
                })
                .map(e -> processPurchaseEvent(e, taxYear))
                .toList();

        return EsppPurchaseReport.fromPurchases(taxYear, taxablePurchases);
    }
    
    private TaxableEsppPurchase processPurchaseEvent(EsppPurchaseEvent event, int taxYear) {
        BigDecimal purchaseDateRate = exchangeRateProvider.getRate(event.purchaseDate());
        BigDecimal yearlyAvgRate = exchangeRateProvider.getYearlyAverageRate(taxYear);
        
        BigDecimal czkByDate = event.discountBenefitUsd().multiply(purchaseDateRate);
        BigDecimal czkByYearlyAvg = event.discountBenefitUsd().multiply(yearlyAvgRate);
        
        logPurchaseEvent(event, purchaseDateRate, czkByDate, yearlyAvgRate, czkByYearlyAvg);
        
        return new TaxableEsppPurchase(event, purchaseDateRate, yearlyAvgRate, czkByDate, czkByYearlyAvg);
    }
    
    private void logPurchaseEvent(EsppPurchaseEvent event, BigDecimal rate, BigDecimal czkByDate,
                                   BigDecimal yearlyRate, BigDecimal czkByYearly) {
        LOG.info(() -> String.format(
                "─── ESPP Purchase %s | %s ───",
                event.grantNumber(), event.purchaseDate()
        ));
        LOG.info(() -> String.format(
                "  %d shares @ $%.2f (FMV $%.2f) | discount $%.2f/sh × %d = $%,.2f",
                event.quantity(),
                event.purchasePrice().doubleValue(),
                event.purchaseDateFmv().doubleValue(),
                event.discountPerShare().doubleValue(),
                event.quantity(),
                event.discountBenefitUsd().doubleValue()
        ));
        LOG.info(() -> String.format(
                "  rate %.3f → %,.2f CZK | yearly @%.3f → %,.2f CZK",
                rate.doubleValue(),
                czkByDate.doubleValue(),
                yearlyRate.doubleValue(),
                czkByYearly.doubleValue()
        ));
    }
}

