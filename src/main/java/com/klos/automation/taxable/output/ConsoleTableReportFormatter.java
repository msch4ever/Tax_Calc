package com.klos.automation.taxable.output;

import com.klos.automation.taxable.calculator.HoldingPeriodCalculator;
import com.klos.automation.taxable.model.TaxReport;
import com.klos.automation.taxable.model.TaxableTransaction;
import com.klos.automation.taxable.model.TaxableVestEvent;
import com.klos.automation.taxable.model.VestingReport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Formats tax report as ASCII table.
 * Single responsibility: convert report to formatted string.
 */
public class ConsoleTableReportFormatter implements ReportFormatter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SEPARATOR = "─".repeat(130);
    private static final String DOUBLE_SEPARATOR = "═".repeat(130);
    private static final String LINE_BREAK = System.lineSeparator();

    private final HoldingPeriodCalculator holdingCalculator;

    public ConsoleTableReportFormatter() {
        this.holdingCalculator = new HoldingPeriodCalculator();
    }

    @Override
    public String format(VestingReport vestingReport, TaxReport sellReport) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, vestingReport, sellReport);
        appendExchangeRateTable(sb, vestingReport, sellReport);
        appendVestingTable(sb, vestingReport);
        appendSellingTable(sb, sellReport);
        appendSummary(sb, vestingReport, sellReport);
        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, VestingReport vestingReport, TaxReport sellReport) {
        sb.append(LINE_BREAK);
        sb.append(DOUBLE_SEPARATOR).append(LINE_BREAK);
        sb.append(String.format("  RSU TAX REPORT - Year %d%n", vestingReport.taxYear()));
        sb.append(String.format("  Vesting Events: %d | Sell Transactions: %d (Exempt: %d)%n",
                vestingReport.vestEvents().size(), sellReport.totalTransactions(), sellReport.exemptTransactions()));
        sb.append(DOUBLE_SEPARATOR).append(LINE_BREAK);
        sb.append(LINE_BREAK);
    }

    private void appendExchangeRateTable(StringBuilder sb, VestingReport vestingReport, TaxReport sellReport) {
        // Collect all unique dates and their rates
        Map<LocalDate, BigDecimal> dateRates = new TreeMap<>();
        BigDecimal yearlyRate = null;

        for (TaxableVestEvent v : vestingReport.vestEvents()) {
            dateRates.put(v.vestDate(), v.vestDateRate());
            if (yearlyRate == null) {
                yearlyRate = v.yearlyAvgRate();
            }
        }
        for (TaxableTransaction t : sellReport.transactions()) {
            dateRates.put(t.vestDate(), t.vestDateRate());
            dateRates.put(t.sellDate(), t.sellDateRate());
            if (yearlyRate == null) {
                yearlyRate = t.yearlyAvgRate();
            }
        }

        sb.append("EXCHANGE RATES (USD/CZK)").append(LINE_BREAK);
        sb.append("─".repeat(40)).append(LINE_BREAK);

        for (Map.Entry<LocalDate, BigDecimal> entry : dateRates.entrySet()) {
            sb.append(String.format("  %s    %s%n",
                    entry.getKey().format(DATE_FMT),
                    formatRate(entry.getValue())));
        }

        sb.append("─".repeat(40)).append(LINE_BREAK);
        if (yearlyRate != null) {
            sb.append(String.format("  Yearly Avg      %s%n", formatRate(yearlyRate)));
        }
        sb.append(LINE_BREAK);
    }

    private void appendVestingTable(StringBuilder sb, VestingReport report) {
        sb.append("TABLE 1: VESTING INCOME (Taxable as Ordinary Income)").append(LINE_BREAK);
        sb.append(SEPARATOR).append(LINE_BREAK);

        // Header
        sb.append(String.format("%-12s %-12s %8s %12s %9s %18s %18s%n",
                "Grant", "Vest Date", "Shares", "USD", "Rate", "CZK (daily)", "CZK (yearly)"));
        sb.append("─".repeat(100)).append(LINE_BREAK);

        // Rows sorted by vest date ASC
        List<TaxableVestEvent> sorted = report.vestEvents().stream()
                .sorted(Comparator.comparing(TaxableVestEvent::vestDate))
                .toList();

        for (TaxableVestEvent v : sorted) {
            sb.append(String.format("%-12s %-12s %8d %12s %9s %18s %18s%n",
                    truncate(v.grantNumber(), 12),
                    v.vestDate().format(DATE_FMT),
                    v.quantity(),
                    formatMoney(v.taxableGainUsd()),
                    formatRate(v.vestDateRate()),
                    formatMoney(v.taxableGainCzkByDate()),
                    formatMoney(v.taxableGainCzkByYearlyAvg())
            ));
        }

        // Totals
        sb.append("─".repeat(100)).append(LINE_BREAK);
        sb.append(String.format("%-12s %-12s %8d %12s %9s %18s %18s%n",
                "TOTAL", "", report.totalShares(),
                formatMoney(report.totalTaxableGainUsd()), "",
                formatMoney(report.totalTaxableGainCzkByDate()),
                formatMoney(report.totalTaxableGainCzkByYearlyAvg())
        ));
        sb.append(SEPARATOR).append(LINE_BREAK);
        sb.append(LINE_BREAK);
    }

    private void appendSellingTable(StringBuilder sb, TaxReport report) {
        sb.append("TABLE 2: CAPITAL GAINS/LOSSES (From Selling RSU)").append(LINE_BREAK);
        sb.append(SEPARATOR).append(LINE_BREAK);

        // Header
        sb.append(String.format("%-12s %-12s %-12s %-8s %12s %9s %15s %15s %7s%n",
                "Grant", "Vest Date", "Sell Date", "Holding",
                "Gain USD", "SellRate", "Gain CZK(date)", "Gain CZK(avg)", "Exempt"));
        sb.append("─".repeat(115)).append(LINE_BREAK);

        // Rows sorted by sell date ASC
        List<TaxableTransaction> sorted = report.transactions().stream()
                .sorted(Comparator.comparing(TaxableTransaction::sellDate))
                .toList();

        for (TaxableTransaction t : sorted) {
            sb.append(String.format("%-12s %-12s %-12s %-8s %12s %9s %15s %15s %7s%n",
                    truncate(t.grantNumber(), 12),
                    t.vestDate().format(DATE_FMT),
                    t.sellDate().format(DATE_FMT),
                    holdingCalculator.formatHoldingPeriod(t.holdingPeriod()),
                    formatMoney(t.capitalGainLossUsd()),
                    formatRate(t.sellDateRate()),
                    formatMoney(t.capitalGainLossCzkByDate()),
                    formatMoney(t.capitalGainLossCzkByYearlyAvg()),
                    t.capitalGainsExempt() ? "YES" : "no"
            ));
        }

        // Totals
        sb.append("─".repeat(115)).append(LINE_BREAK);
        sb.append(String.format("%-12s %-12s %-12s %-8s %12s %9s %15s %15s%n",
                "TOTAL", "", "", "",
                formatMoney(report.totalCapitalGainLossUsd()), "",
                formatMoney(report.totalCapitalGainLossCzkByDate()),
                formatMoney(report.totalCapitalGainLossCzkByYearlyAvg())
        ));
        sb.append(String.format("%-12s %-12s %-12s %-8s %12s %9s %15s %15s%n",
                "TAXABLE", "(excl exempt)", "", "",
                formatMoney(report.totalTaxableCapitalGainUsd()), "",
                formatMoney(report.totalTaxableCapitalGainCzkByDate()),
                formatMoney(report.totalTaxableCapitalGainCzkByYearlyAvg())
        ));
        sb.append(SEPARATOR).append(LINE_BREAK);
        sb.append(LINE_BREAK);
    }
    
    private void appendSummary(StringBuilder sb, VestingReport vestingReport, TaxReport sellReport) {
        sb.append("FINAL TAX SUMMARY").append(LINE_BREAK);
        sb.append(DOUBLE_SEPARATOR).append(LINE_BREAK);
        sb.append(LINE_BREAK);

        // Calculate combined totals
        BigDecimal vestUsd = vestingReport.totalTaxableGainUsd();
        BigDecimal vestCzkDaily = vestingReport.totalTaxableGainCzkByDate();
        BigDecimal vestCzkYearly = vestingReport.totalTaxableGainCzkByYearlyAvg();

        BigDecimal gainUsd = sellReport.totalTaxableCapitalGainUsd();
        BigDecimal gainCzkDaily = sellReport.totalTaxableCapitalGainCzkByDate();
        BigDecimal gainCzkYearly = sellReport.totalTaxableCapitalGainCzkByYearlyAvg();

        // If capital gain is negative, it's not taxable but still reportable
        BigDecimal taxableGainCzkDaily = gainCzkDaily.compareTo(BigDecimal.ZERO) > 0 ? gainCzkDaily : BigDecimal.ZERO;
        BigDecimal taxableGainCzkYearly = gainCzkYearly.compareTo(BigDecimal.ZERO) > 0 ? gainCzkYearly : BigDecimal.ZERO;

        BigDecimal totalCzkDaily = vestCzkDaily.add(taxableGainCzkDaily);
        BigDecimal totalCzkYearly = vestCzkYearly.add(taxableGainCzkYearly);

        // Summary table
        sb.append(String.format("%-35s %18s %18s %18s%n", "", "USD", "CZK (daily)", "CZK (yearly)"));
        sb.append("─".repeat(95)).append(LINE_BREAK);

        sb.append(String.format("%-35s %18s %18s %18s%n",
                "1. Vesting Income (always taxable)",
                formatMoney(vestUsd),
                formatMoney(vestCzkDaily),
                formatMoney(vestCzkYearly)));

        sb.append(String.format("%-35s %18s %18s %18s%n",
                "2. Capital Gain/Loss (from sells)",
                formatMoney(gainUsd),
                formatMoney(gainCzkDaily),
                formatMoney(gainCzkYearly)));

        // Note about negative gains
        if (gainCzkDaily.compareTo(BigDecimal.ZERO) < 0 || gainCzkYearly.compareTo(BigDecimal.ZERO) < 0) {
            sb.append(String.format("   %-32s %18s %18s %18s%n",
                    "(Loss - report but not taxable)",
                    "",
                    gainCzkDaily.compareTo(BigDecimal.ZERO) < 0 ? "→ 0" : "",
                    gainCzkYearly.compareTo(BigDecimal.ZERO) < 0 ? "→ 0" : ""));
        }

        sb.append("─".repeat(95)).append(LINE_BREAK);
        sb.append(String.format("%-35s %18s %18s %18s%n",
                "TOTAL TAXABLE",
                formatMoney(vestUsd.add(gainUsd.compareTo(BigDecimal.ZERO) > 0 ? gainUsd : BigDecimal.ZERO)),
                formatMoney(totalCzkDaily),
                formatMoney(totalCzkYearly)));
        sb.append(LINE_BREAK);

        // Comparison
        sb.append(DOUBLE_SEPARATOR).append(LINE_BREAK);
        BigDecimal diff = totalCzkDaily.subtract(totalCzkYearly);
        String better = diff.compareTo(BigDecimal.ZERO) > 0 ? "Yearly Average" : "Transaction Date";
        sb.append(String.format("  → Better option: %s rate (saves %s CZK)%n", better, formatMoney(diff.abs())));
        sb.append(DOUBLE_SEPARATOR).append(LINE_BREAK);
        sb.append(LINE_BREAK);
    }
    
    private String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String formatRate(BigDecimal rate) {
        return String.format("%.3f", rate);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
    
    @Override
    public String getName() {
        return "Console Table Formatter";
    }
}

