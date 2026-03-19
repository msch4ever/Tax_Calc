package com.klos.automation.taxable.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Complete tax report for a given tax year.
 * Contains all transactions and summary totals.
 */
public record TaxReport(
        int taxYear,
        PlanType planType,
        List<TaxableTransaction> transactions,

        // USD totals
        BigDecimal totalPurchaseCostUsd,        // Total FMV at purchase (cost basis)
        BigDecimal totalSaleProceedsUsd,        // Total amount received from sales
        BigDecimal totalVestIncomeUsd,
        BigDecimal totalCapitalGainLossUsd,
        BigDecimal totalTaxableCapitalGainUsd,
        BigDecimal totalTaxableUsd,

        // CZK totals using transaction date rates
        BigDecimal totalPurchaseCostCzkByDate,  // Purchase cost in CZK (daily rates)
        BigDecimal totalSaleProceedsCzkByDate,  // Sale proceeds in CZK (daily rates)
        BigDecimal totalVestIncomeCzkByDate,
        BigDecimal totalCapitalGainLossCzkByDate,
        BigDecimal totalTaxableCapitalGainCzkByDate,
        BigDecimal totalTaxableCzkByDate,

        // CZK totals using yearly average rate
        BigDecimal totalPurchaseCostCzkByYearlyAvg,  // Purchase cost in CZK (yearly rates)
        BigDecimal totalSaleProceedsCzkByYearlyAvg,  // Sale proceeds in CZK (yearly rates)
        BigDecimal totalVestIncomeCzkByYearlyAvg,
        BigDecimal totalCapitalGainLossCzkByYearlyAvg,
        BigDecimal totalTaxableCapitalGainCzkByYearlyAvg,
        BigDecimal totalTaxableCzkByYearlyAvg,

        // Counts
        int totalTransactions,
        int exemptTransactions
) {
    
    /**
     * Create a tax report from a list of taxable transactions.
     */
    public static TaxReport fromTransactions(int taxYear, PlanType planType, List<TaxableTransaction> transactions) {
        BigDecimal totalPurchaseCostUsd = BigDecimal.ZERO;
        BigDecimal totalSaleProceedsUsd = BigDecimal.ZERO;
        BigDecimal totalVestIncomeUsd = BigDecimal.ZERO;
        BigDecimal totalCapitalGainLossUsd = BigDecimal.ZERO;
        BigDecimal totalTaxableCapitalGainUsd = BigDecimal.ZERO;

        BigDecimal totalPurchaseCostCzkByDate = BigDecimal.ZERO;
        BigDecimal totalSaleProceedsCzkByDate = BigDecimal.ZERO;
        BigDecimal totalVestIncomeCzkByDate = BigDecimal.ZERO;
        BigDecimal totalCapitalGainLossCzkByDate = BigDecimal.ZERO;
        BigDecimal totalTaxableCapitalGainCzkByDate = BigDecimal.ZERO;

        BigDecimal totalPurchaseCostCzkByYearlyAvg = BigDecimal.ZERO;
        BigDecimal totalSaleProceedsCzkByYearlyAvg = BigDecimal.ZERO;
        BigDecimal totalVestIncomeCzkByYearlyAvg = BigDecimal.ZERO;
        BigDecimal totalCapitalGainLossCzkByYearlyAvg = BigDecimal.ZERO;
        BigDecimal totalTaxableCapitalGainCzkByYearlyAvg = BigDecimal.ZERO;

        int exemptCount = 0;

        for (TaxableTransaction t : transactions) {
            // Purchase cost = FMV at vest/purchase × quantity (cost basis for capital gains)
            BigDecimal purchaseCostUsd = t.vestPricePerShare().multiply(BigDecimal.valueOf(t.quantity()));
            totalPurchaseCostUsd = totalPurchaseCostUsd.add(purchaseCostUsd);
            totalSaleProceedsUsd = totalSaleProceedsUsd.add(t.sellProceedsUsd());

            // CZK amounts using daily rates
            BigDecimal purchaseCostCzkByDate = purchaseCostUsd.multiply(t.vestDateRate());
            BigDecimal saleProceedsCzkByDate = t.sellProceedsUsd().multiply(t.sellDateRate());
            totalPurchaseCostCzkByDate = totalPurchaseCostCzkByDate.add(purchaseCostCzkByDate);
            totalSaleProceedsCzkByDate = totalSaleProceedsCzkByDate.add(saleProceedsCzkByDate);

            // CZK amounts using yearly average rate
            BigDecimal purchaseCostCzkByYearlyAvg = purchaseCostUsd.multiply(t.yearlyAvgRate());
            BigDecimal saleProceedsCzkByYearlyAvg = t.sellProceedsUsd().multiply(t.yearlyAvgRate());
            totalPurchaseCostCzkByYearlyAvg = totalPurchaseCostCzkByYearlyAvg.add(purchaseCostCzkByYearlyAvg);
            totalSaleProceedsCzkByYearlyAvg = totalSaleProceedsCzkByYearlyAvg.add(saleProceedsCzkByYearlyAvg);

            totalVestIncomeUsd = totalVestIncomeUsd.add(t.vestIncomeUsd());
            totalCapitalGainLossUsd = totalCapitalGainLossUsd.add(t.capitalGainLossUsd());
            totalTaxableCapitalGainUsd = totalTaxableCapitalGainUsd.add(t.taxableCapitalGainUsd());

            totalVestIncomeCzkByDate = totalVestIncomeCzkByDate.add(t.vestIncomeCzkByDate());
            totalCapitalGainLossCzkByDate = totalCapitalGainLossCzkByDate.add(t.capitalGainLossCzkByDate());
            totalTaxableCapitalGainCzkByDate = totalTaxableCapitalGainCzkByDate.add(t.taxableCapitalGainCzkByDate());

            totalVestIncomeCzkByYearlyAvg = totalVestIncomeCzkByYearlyAvg.add(t.vestIncomeCzkByYearlyAvg());
            totalCapitalGainLossCzkByYearlyAvg = totalCapitalGainLossCzkByYearlyAvg.add(t.capitalGainLossCzkByYearlyAvg());
            totalTaxableCapitalGainCzkByYearlyAvg = totalTaxableCapitalGainCzkByYearlyAvg.add(t.taxableCapitalGainCzkByYearlyAvg());

            if (t.capitalGainsExempt()) {
                exemptCount++;
            }
        }
        
        return new TaxReport(
                taxYear,
                planType,
                transactions,
                totalPurchaseCostUsd,
                totalSaleProceedsUsd,
                totalVestIncomeUsd,
                totalCapitalGainLossUsd,
                totalTaxableCapitalGainUsd,
                totalVestIncomeUsd.add(totalTaxableCapitalGainUsd),
                totalPurchaseCostCzkByDate,
                totalSaleProceedsCzkByDate,
                totalVestIncomeCzkByDate,
                totalCapitalGainLossCzkByDate,
                totalTaxableCapitalGainCzkByDate,
                totalVestIncomeCzkByDate.add(totalTaxableCapitalGainCzkByDate),
                totalPurchaseCostCzkByYearlyAvg,
                totalSaleProceedsCzkByYearlyAvg,
                totalVestIncomeCzkByYearlyAvg,
                totalCapitalGainLossCzkByYearlyAvg,
                totalTaxableCapitalGainCzkByYearlyAvg,
                totalVestIncomeCzkByYearlyAvg.add(totalTaxableCapitalGainCzkByYearlyAvg),
                transactions.size(),
                exemptCount
        );
    }

    /**
     * Create an empty report for when a plan type is not selected.
     */
    public static TaxReport empty(int taxYear) {
        return new TaxReport(
                taxYear,
                null,
                List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0
        );
    }
}

