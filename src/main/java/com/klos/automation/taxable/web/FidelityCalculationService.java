package com.klos.automation.taxable.web;

import com.klos.automation.taxable.calculator.RsuTaxCalculator;
import com.klos.automation.taxable.calculator.VestingCalculator;
import com.klos.automation.taxable.exchange.CnbExchangeRateProvider;
import com.klos.automation.taxable.exchange.CombinedExchangeRateProvider;
import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.model.*;
import com.klos.automation.taxable.parser.FidelityPdfParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing Fidelity NetBenefits PDF exports.
 *
 * <p>Fidelity PDFs contain stock sale data from which we can derive:
 * <ul>
 *   <li>RSU vesting income (from cost basis = FMV at vest × quantity)</li>
 *   <li>RSU capital gains/losses (from sale proceeds vs cost basis)</li>
 * </ul>
 */
@Service
public class FidelityCalculationService {

    private final FidelityPdfParser pdfParser = new FidelityPdfParser();

    /**
     * Calculate tax report from a Fidelity "Custom transaction summary" PDF.
     *
     * @param pdfStream    the PDF file content
     * @param taxYear      the tax year to filter transactions
     * @return a TaxCalculationResult with RSU vesting + sell reports
     * @throws IOException if the PDF cannot be read
     */
    public TaxCalculationResult calculate(InputStream pdfStream, int taxYear) throws IOException {
        StringBuilder log = new StringBuilder();

        // Setup exchange rate provider (reuses existing infrastructure)
        ExchangeRateProvider rateProvider = createExchangeRateProvider(log);

        // Parse stock sale transactions from PDF
        log.append("\n=== FIDELITY PDF PARSING ===\n");
        List<StockTransaction> transactions = pdfParser.parse(pdfStream);
        log.append(String.format("Parsed %d stock sale transactions from Fidelity PDF%n", transactions.size()));

        // Filter to RSU only (all Fidelity transactions should already be RS)
        List<StockTransaction> rsuTransactions = transactions.stream()
                .filter(t -> t.planType() == PlanType.RS)
                .toList();

        // === VESTING INCOME ===
        // Derive vest events from stock sale transactions.
        // Each sale row has dateAcquired (= vest date) and costBasis (= FMV at vest × qty).
        // We group by vest date to avoid duplicating vest events for shares sold in multiple lots.
        log.append(String.format("%n=== RSU VESTING INCOME (derived from Fidelity PDF) ===%n"));
        List<VestEvent> vestEvents = deriveVestEvents(rsuTransactions, log);

        VestingCalculator vestingCalculator = new VestingCalculator(rateProvider);
        VestingReport vestingReport = vestingCalculator.calculateForYear(vestEvents, taxYear);

        log.append(String.format("Derived %d vest events, %d in tax year %d%n",
                vestEvents.size(), vestingReport.vestEvents().size(), taxYear));
        for (TaxableVestEvent v : vestingReport.vestEvents()) {
            log.append(String.format("  VEST %s | %d shares × $%.2f = $%,.2f | @%.3f → %,.2f CZK (daily) | @%.3f → %,.2f CZK (yearly)%n",
                    v.vestDate(), v.quantity(), v.pricePerShare().doubleValue(),
                    v.taxableGainUsd().doubleValue(),
                    v.vestDateRate().doubleValue(), v.taxableGainCzkByDate().doubleValue(),
                    v.yearlyAvgRate().doubleValue(), v.taxableGainCzkByYearlyAvg().doubleValue()));
        }

        // === CAPITAL GAINS/LOSSES ===
        log.append(String.format("%n=== RSU SELL TRANSACTIONS (Fidelity) ===%n"));
        RsuTaxCalculator calculator = new RsuTaxCalculator(rateProvider);
        TaxReport rsuSellReport = calculator.calculateForYear(rsuTransactions, taxYear);

        for (TaxableTransaction t : rsuSellReport.transactions()) {
            log.append(String.format("SELL %s | Acquired %s | %d shares%n",
                    t.sellDate(), t.vestDate(), t.quantity()));
            log.append(String.format("     Cost: $%,.2f @ %.3f → %,.2f CZK%n",
                    t.vestIncomeUsd().doubleValue(), t.vestDateRate().doubleValue(),
                    t.vestIncomeCzkByDate().doubleValue()));
            log.append(String.format("     Sell: $%,.2f @ %.3f → %,.2f CZK%n",
                    t.sellProceedsUsd().doubleValue(), t.sellDateRate().doubleValue(),
                    t.sellProceedsCzkByDate().doubleValue()));
            log.append(String.format("     GAIN: $%,.2f USD | %,.2f CZK (daily) | %,.2f CZK (yearly)%s%n",
                    t.capitalGainLossUsd().doubleValue(),
                    t.capitalGainLossCzkByDate().doubleValue(),
                    t.capitalGainLossCzkByYearlyAvg().doubleValue(),
                    t.capitalGainsExempt() ? " [EXEMPT - held >3y]" : ""));
        }

        if (rsuSellReport.transactions().isEmpty()) {
            log.append("     (No taxable sell transactions for this year)\n");
        }

        return new TaxCalculationResult(
                vestingReport,                      // RSU vesting income derived from PDF
                rsuSellReport,
                EsppPurchaseReport.empty(taxYear),  // No ESPP data from Fidelity PDF
                TaxReport.empty(taxYear),           // No ESPP sell data
                true,                               // RSU vesting IS processed
                false,                              // ESPP not processed
                log.toString()
        );
    }

    /**
     * Derive VestEvent records from Fidelity stock sale transactions.
     * Groups by vest date + FMV per share to aggregate shares that vested
     * on the same date at the same price but were sold in different lots.
     */
    private List<VestEvent> deriveVestEvents(List<StockTransaction> transactions, StringBuilder log) {
        // Group by (vest date, FMV per share) to get unique vesting events
        record VestKey(LocalDate vestDate, BigDecimal fmvPerShare) {}

        Map<VestKey, List<StockTransaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> new VestKey(t.dateAcquired(), t.vestDateFmv()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<VestEvent> vestEvents = new ArrayList<>();
        int eventNum = 0;

        for (Map.Entry<VestKey, List<StockTransaction>> entry : grouped.entrySet()) {
            eventNum++;
            VestKey key = entry.getKey();
            List<StockTransaction> txs = entry.getValue();

            // Sum quantities across all sell lots for this vest event
            int totalQuantity = txs.stream().mapToInt(StockTransaction::quantity).sum();

            // Taxable gain = FMV at vest × total quantity
            BigDecimal taxableGainUsd = key.fmvPerShare().multiply(BigDecimal.valueOf(totalQuantity));

            String grantNumber = String.format("FID-VEST-%d", eventNum);

            VestEvent event = VestEvent.of(grantNumber, key.vestDate(), totalQuantity, taxableGainUsd);
            vestEvents.add(event);

            log.append(String.format("  Derived vest: %s | %d shares × $%.2f = $%,.2f%n",
                    key.vestDate(), totalQuantity, key.fmvPerShare().doubleValue(), taxableGainUsd.doubleValue()));
        }

        return vestEvents;
    }

    private ExchangeRateProvider createExchangeRateProvider(StringBuilder log) {
        log.append("Using historical GFŘ yearly rates (2019-2025) + CNB daily rates\n");
        log.append("Yearly rates: 2019=22.93, 2020=23.14, 2021=21.72, 2022=23.41, 2023=22.14, 2024=23.28, 2025=21.84\n");
        return new CombinedExchangeRateProvider(new CnbExchangeRateProvider());
    }
}
