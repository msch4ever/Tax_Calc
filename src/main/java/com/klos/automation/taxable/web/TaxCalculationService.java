package com.klos.automation.taxable.web;

import com.klos.automation.taxable.calculator.EsppPurchaseCalculator;
import com.klos.automation.taxable.calculator.RsuTaxCalculator;
import com.klos.automation.taxable.calculator.VestingCalculator;
import com.klos.automation.taxable.exchange.CnbExchangeRateProvider;
import com.klos.automation.taxable.exchange.CombinedExchangeRateProvider;
import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.model.*;
import com.klos.automation.taxable.parser.EsppPurchaseParser;
import com.klos.automation.taxable.parser.ExcelToCsvConverter;
import com.klos.automation.taxable.parser.GainsLossesCsvParser;
import com.klos.automation.taxable.parser.RestrictedStockCsvParser;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TaxCalculationService {

    private final ExcelToCsvConverter excelConverter = new ExcelToCsvConverter();

    public TaxCalculationResult calculate(InputStream benefitHistoryStream,
                                          String benefitHistoryFilename,
                                          InputStream gainsLossesStream,
                                          String gainsLossesFilename,
                                          int taxYear,
                                          BigDecimal yearlyRate,
                                          boolean processRsu,
                                          boolean processEspp) throws IOException {

        StringBuilder log = new StringBuilder();

        // Setup exchange rate provider
        ExchangeRateProvider rateProvider = createExchangeRateProvider(log);

        // Initialize empty reports
        VestingReport rsuVestingReport = VestingReport.empty(taxYear);
        TaxReport rsuSellReport = TaxReport.empty(taxYear);
        EsppPurchaseReport esppPurchaseReport = EsppPurchaseReport.empty(taxYear);
        TaxReport esppSellReport = TaxReport.empty(taxYear);

        // Read benefit history file bytes once (we may need to read it twice for RSU and ESPP)
        byte[] benefitHistoryBytes = benefitHistoryStream.readAllBytes();
        byte[] gainsLossesBytes = gainsLossesStream.readAllBytes();

        // Process RSU if selected
        if (processRsu) {
            rsuVestingReport = calculateRsuVesting(benefitHistoryBytes, benefitHistoryFilename, taxYear, rateProvider, log);
            rsuSellReport = calculateSellTransactions(gainsLossesBytes, gainsLossesFilename, taxYear, rateProvider, log, PlanType.RS, "RSU");
        }

        // Process ESPP if selected
        if (processEspp) {
            esppPurchaseReport = calculateEsppPurchases(benefitHistoryBytes, benefitHistoryFilename, taxYear, rateProvider, log);
            esppSellReport = calculateSellTransactions(gainsLossesBytes, gainsLossesFilename, taxYear, rateProvider, log, PlanType.ESPP, "ESPP");
        }

        return new TaxCalculationResult(
                rsuVestingReport, rsuSellReport,
                esppPurchaseReport, esppSellReport,
                processRsu, processEspp,
                log.toString()
        );
    }

    private VestingReport calculateRsuVesting(byte[] fileBytes, String filename, int taxYear,
                                               ExchangeRateProvider rateProvider, StringBuilder log) throws IOException {
        log.append("\n=== RSU VESTING EVENTS ===\n");

        InputStream csvStream = convertIfExcel(new ByteArrayInputStream(fileBytes), filename, "Restricted Stock");
        RestrictedStockCsvParser parser = new RestrictedStockCsvParser();
        List<VestEvent> vestEvents = parser.parse(csvStream);

        VestingCalculator calculator = new VestingCalculator(rateProvider);
        VestingReport report = calculator.calculateForYear(vestEvents, taxYear);

        for (TaxableVestEvent v : report.vestEvents()) {
            log.append(String.format("VEST %s | Grant %s | %d shares × $%.2f = $%,.2f%n",
                    v.vestDate(), v.grantNumber(), v.quantity(),
                    v.taxableGainUsd().divide(BigDecimal.valueOf(v.quantity()), 2, java.math.RoundingMode.HALF_UP).doubleValue(),
                    v.taxableGainUsd().doubleValue()));
            log.append(String.format("     Rate %.3f → %,.2f CZK (daily) | Rate %.3f → %,.2f CZK (yearly)%n",
                    v.vestDateRate().doubleValue(), v.taxableGainCzkByDate().doubleValue(),
                    v.yearlyAvgRate().doubleValue(), v.taxableGainCzkByYearlyAvg().doubleValue()));
        }

        return report;
    }

    private EsppPurchaseReport calculateEsppPurchases(byte[] fileBytes, String filename, int taxYear,
                                                       ExchangeRateProvider rateProvider, StringBuilder log) throws IOException {
        log.append("\n=== ESPP PURCHASE EVENTS ===\n");

        InputStream csvStream = convertIfExcel(new ByteArrayInputStream(fileBytes), filename, "ESPP");
        EsppPurchaseParser parser = new EsppPurchaseParser();
        List<EsppPurchaseEvent> purchases = parser.parse(csvStream);

        EsppPurchaseCalculator calculator = new EsppPurchaseCalculator(rateProvider);
        EsppPurchaseReport report = calculator.calculateForYear(purchases, taxYear);

        for (TaxableEsppPurchase p : report.purchases()) {
            log.append(String.format("PURCHASE %s | %s | %d shares @ $%.2f (FMV $%.2f)%n",
                    p.grantNumber(), p.purchaseDate(), p.quantity(),
                    p.purchasePrice().doubleValue(), p.purchaseDateFmv().doubleValue()));
            log.append(String.format("     Discount: $%.2f/share × %d = $%,.2f%n",
                    p.discountPerShare().doubleValue(), p.quantity(), p.discountBenefitUsd().doubleValue()));
            log.append(String.format("     Rate %.3f → %,.2f CZK (daily) | Rate %.3f → %,.2f CZK (yearly)%n",
                    p.purchaseDateRate().doubleValue(), p.discountBenefitCzkByDate().doubleValue(),
                    p.yearlyAvgRate().doubleValue(), p.discountBenefitCzkByYearlyAvg().doubleValue()));
        }

        return report;
    }

    private TaxReport calculateSellTransactions(byte[] fileBytes, String filename, int taxYear,
                                                 ExchangeRateProvider rateProvider, StringBuilder log,
                                                 PlanType planType, String planName) throws IOException {
        log.append(String.format("%n=== %s SELL TRANSACTIONS ===%n", planName));

        InputStream csvStream = convertIfExcel(new ByteArrayInputStream(fileBytes), filename, null);
        GainsLossesCsvParser parser = new GainsLossesCsvParser();
        List<StockTransaction> allTransactions = parser.parse(csvStream);

        // Filter by plan type
        List<StockTransaction> filteredTransactions = allTransactions.stream()
                .filter(t -> t.planType() == planType)
                .toList();

        RsuTaxCalculator calculator = new RsuTaxCalculator(rateProvider);
        TaxReport report = calculator.calculateForYear(filteredTransactions, taxYear);

        for (TaxableTransaction t : report.transactions()) {
            log.append(String.format("SELL %s | Grant %s | Vest: %s%n",
                    t.sellDate(), t.grantNumber(), t.vestDate()));
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

        if (report.transactions().isEmpty()) {
            log.append("     (No taxable transactions for this year)\n");
        }

        return report;
    }

    private ExchangeRateProvider createExchangeRateProvider(StringBuilder log) {
        log.append("Using historical GFŘ yearly rates (2019-2025) + CNB daily rates\n");
        log.append("Yearly rates: 2019=22.93, 2020=23.14, 2021=21.72, 2022=23.41, 2023=22.14, 2024=23.28, 2025=21.84\n");
        return new CombinedExchangeRateProvider(new CnbExchangeRateProvider());
    }

    private InputStream convertIfExcel(InputStream inputStream, String filename, String sheetName) throws IOException {
        if (ExcelToCsvConverter.isExcelFile(filename)) {
            return excelConverter.convertToCsv(inputStream, sheetName);
        }
        return inputStream;
    }
}

