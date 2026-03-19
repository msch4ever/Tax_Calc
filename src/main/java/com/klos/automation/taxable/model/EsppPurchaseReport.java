package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Report containing all ESPP purchase events for a tax year.
 * This represents the taxable income from ESPP discount benefit (always taxable at purchase).
 */
public record EsppPurchaseReport(
        int taxYear,
        List<TaxableEsppPurchase> purchases,
        
        // Totals
        int totalShares,
        BigDecimal totalDiscountBenefitUsd,
        BigDecimal totalDiscountBenefitCzkByDate,
        BigDecimal totalDiscountBenefitCzkByYearlyAvg
) {
    
    /**
     * Create an EsppPurchaseReport from a list of taxable purchases.
     */
    public static EsppPurchaseReport fromPurchases(int taxYear, List<TaxableEsppPurchase> purchases) {
        int totalShares = purchases.stream()
                .mapToInt(TaxableEsppPurchase::quantity)
                .sum();
        
        BigDecimal totalUsd = purchases.stream()
                .map(TaxableEsppPurchase::discountBenefitUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCzkByDate = purchases.stream()
                .map(TaxableEsppPurchase::discountBenefitCzkByDate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCzkByYearlyAvg = purchases.stream()
                .map(TaxableEsppPurchase::discountBenefitCzkByYearlyAvg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new EsppPurchaseReport(taxYear, purchases, totalShares, totalUsd, totalCzkByDate, totalCzkByYearlyAvg);
    }
    
    /**
     * Create an empty report for when ESPP is not selected.
     */
    public static EsppPurchaseReport empty(int taxYear) {
        return new EsppPurchaseReport(taxYear, List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

