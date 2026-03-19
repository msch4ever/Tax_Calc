package com.klos.automation.taxable.calculator;

import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.filter.TransactionFilter;
import com.klos.automation.taxable.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.logging.Logger;

/**
 * Main calculator for RSU taxable amounts.
 * Processes stock transactions and generates tax reports.
 */
public class RsuTaxCalculator {

    private static final Logger LOG = Logger.getLogger(RsuTaxCalculator.class.getName());

    private final ExchangeRateProvider exchangeRateProvider;
    private final HoldingPeriodCalculator holdingPeriodCalculator;

    public RsuTaxCalculator(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.holdingPeriodCalculator = new HoldingPeriodCalculator();
    }
    
    /**
     * Calculate tax report for transactions in a specific year.
     * Transactions should already be filtered by plan type before calling this method.
     *
     * @param transactions pre-filtered transactions (by plan type)
     * @param taxYear      the calendar year for tax reporting
     * @return the tax report
     */
    public TaxReport calculateForYear(List<StockTransaction> transactions, int taxYear) {
        // Filter by sell year only - plan type filtering should happen before this call
        List<StockTransaction> yearTransactions = transactions.stream()
                .filter(t -> t.sellYear() == taxYear)
                .toList();

        // Sort by sell date first, then by grant number
        List<TaxableTransaction> taxableTransactions = yearTransactions.stream()
                .sorted((a, b) -> {
                    int dateCompare = a.dateSold().compareTo(b.dateSold());
                    if (dateCompare != 0) return dateCompare;
                    return a.grantNumber().compareTo(b.grantNumber());
                })
                .map(t -> processTransaction(t, taxYear))
                .toList();

        // Determine plan type from transactions (or default to RS if empty)
        PlanType planType = yearTransactions.isEmpty() ? PlanType.RS : yearTransactions.get(0).planType();

        return TaxReport.fromTransactions(taxYear, planType, taxableTransactions);
    }
    
    /**
     * Process a single transaction and calculate all tax-related amounts.
     *
     * CZK Capital Gain/Loss calculation (per Czech tax law - slides 38-42):
     * - CZK Daily: (sellProceedsUsd × sellRate) - (costBasisUsd × vestRate) [UNOFFICIAL]
     * - CZK Yearly (OFFICIAL): (sellProceedsUsd × sellYearRate) - (costBasisUsd × vestYearRate)
     *
     * CRITICAL: For yearly calculation, use DIFFERENT rates for different years:
     * - Sale proceeds: yearly rate for SALE year
     * - Cost basis: yearly rate for VEST/PURCHASE year
     *
     * Cost Basis (Acquisition Price):
     * - RSU: FMV at vest
     * - ESPP: FMV at purchase (because discount was already taxed as ordinary income)
     */
    private TaxableTransaction processTransaction(StockTransaction transaction, int taxYear) {
        LocalDate vestDate = transaction.vestDate() != null
                ? transaction.vestDate()
                : transaction.dateAcquired();
        LocalDate sellDate = transaction.dateSold();

        Period holdingPeriod = holdingPeriodCalculator.calculateHoldingPeriod(vestDate, sellDate);
        boolean exempt = holdingPeriodCalculator.isCapitalGainsExempt(vestDate, sellDate);

        // USD amounts from E-Trade
        // Cost basis (Acquisition Price per Czech tax law):
        // - RSU: FMV at vest × quantity = ordinaryIncomeRecognized
        // - ESPP: FMV at purchase × quantity (NOT the discounted price!)
        //         Because the discount was already taxed as ordinary income
        BigDecimal costBasisUsd;
        if (transaction.isEspp()) {
            // ESPP: Cost basis = FMV at purchase × quantity
            costBasisUsd = transaction.vestDateFmv().multiply(BigDecimal.valueOf(transaction.quantity()));
        } else {
            // RSU: Cost basis = FMV at vest × quantity = ordinaryIncomeRecognized
            costBasisUsd = transaction.ordinaryIncomeRecognized();
        }
        BigDecimal sellProceedsUsd = transaction.totalProceeds();              // Sale proceeds
        BigDecimal capitalGainLossUsd = transaction.gainLoss();                // E-Trade calculated

        // Get vest and sell years
        int vestYear = vestDate.getYear();
        int sellYear = sellDate.getYear();

        // Get daily rates (for unofficial "daily" calculation)
        BigDecimal vestDateRate = exchangeRateProvider.getRate(vestDate);
        BigDecimal sellDateRate = exchangeRateProvider.getRate(sellDate);

        // Get yearly average rates - DIFFERENT rates for different years!
        BigDecimal vestYearRate = exchangeRateProvider.getYearlyAverageRate(vestYear);
        BigDecimal sellYearRate = exchangeRateProvider.getYearlyAverageRate(sellYear);

        // CZK Daily (UNOFFICIAL - for comparison only)
        BigDecimal costBasisCzkByDate = costBasisUsd.multiply(vestDateRate);
        BigDecimal sellProceedsCzkByDate = sellProceedsUsd.multiply(sellDateRate);
        BigDecimal capitalGainLossCzkByDate = sellProceedsCzkByDate.subtract(costBasisCzkByDate);

        // CZK Yearly (OFFICIAL method per Czech tax law)
        // Cost basis: FMV × quantity × vestYearRate
        // Proceeds: sellPrice × quantity × sellYearRate
        BigDecimal costBasisCzkByYearly = costBasisUsd.multiply(vestYearRate);
        BigDecimal sellProceedsCzkByYearly = sellProceedsUsd.multiply(sellYearRate);
        BigDecimal capitalGainLossCzkByYearlyAvg = sellProceedsCzkByYearly.subtract(costBasisCzkByYearly);

        // Log conversion details
        logTransaction(transaction, vestDate, sellDate,
                vestDateRate, sellDateRate, vestYearRate, sellYearRate,
                costBasisCzkByDate, sellProceedsCzkByDate,
                capitalGainLossCzkByDate, capitalGainLossCzkByYearlyAvg);

        return new TaxableTransaction(
                transaction,
                holdingPeriod,
                exempt,
                costBasisUsd,         // This is acquisition price (FMV × qty)
                sellProceedsUsd,
                capitalGainLossUsd,
                vestDateRate,
                sellDateRate,
                sellYearRate,         // Primary display rate (sell year for capital gains)
                costBasisCzkByDate,
                sellProceedsCzkByDate,
                capitalGainLossCzkByDate,
                costBasisCzkByYearly, // For display: cost basis in CZK (yearly method)
                capitalGainLossCzkByYearlyAvg
        );
    }

    private void logTransaction(StockTransaction tx, LocalDate vestDate, LocalDate sellDate,
                                  BigDecimal vestDailyRate, BigDecimal sellDailyRate,
                                  BigDecimal vestYearlyRate, BigDecimal sellYearlyRate,
                                  BigDecimal costBasisCzkDaily, BigDecimal sellCzkDaily,
                                  BigDecimal gainCzkDaily, BigDecimal gainCzkYearly) {
        LOG.info(() -> String.format(
                "─── Grant %s | %d shares ───",
                tx.grantNumber(), tx.quantity()
        ));
        LOG.info(() -> String.format(
                "  BASIS %s: $%,.2f | daily rate %.3f → %,.2f CZK | yearly(%d) @%.3f",
                vestDate,
                tx.ordinaryIncomeRecognized().doubleValue(),
                vestDailyRate.doubleValue(),
                costBasisCzkDaily.doubleValue(),
                vestDate.getYear(),
                vestYearlyRate.doubleValue()
        ));
        LOG.info(() -> String.format(
                "  SELL  %s: %d × $%.2f = $%,.2f | daily rate %.3f → %,.2f CZK | yearly(%d) @%.3f",
                sellDate, tx.quantity(),
                tx.proceedsPerShare().doubleValue(),
                tx.totalProceeds().doubleValue(),
                sellDailyRate.doubleValue(),
                sellCzkDaily.doubleValue(),
                sellDate.getYear(),
                sellYearlyRate.doubleValue()
        ));
        LOG.info(() -> String.format(
                "  GAIN  USD: $%,.2f | CZK(daily): %,.2f | CZK(yearly): %,.2f",
                tx.gainLoss().doubleValue(),
                gainCzkDaily.doubleValue(),
                gainCzkYearly.doubleValue()
        ));
    }
}

