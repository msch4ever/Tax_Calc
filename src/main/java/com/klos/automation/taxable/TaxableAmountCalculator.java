package com.klos.automation.taxable;

import com.klos.automation.taxable.calculator.RsuTaxCalculator;
import com.klos.automation.taxable.calculator.VestingCalculator;
import com.klos.automation.taxable.exchange.CnbExchangeRateProvider;
import com.klos.automation.taxable.exchange.CombinedExchangeRateProvider;
import com.klos.automation.taxable.exchange.ExchangeRateProvider;
import com.klos.automation.taxable.model.StockTransaction;
import com.klos.automation.taxable.model.TaxReport;
import com.klos.automation.taxable.model.VestEvent;
import com.klos.automation.taxable.model.VestingReport;
import com.klos.automation.taxable.output.ConsolePrinter;
import com.klos.automation.taxable.output.ConsoleTableReportFormatter;
import com.klos.automation.taxable.output.ReportFormatter;
import com.klos.automation.taxable.output.ReportPrinter;
import com.klos.automation.taxable.parser.GainsLossesCsvParser;
import com.klos.automation.taxable.parser.RestrictedStockCsvParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.LogManager;

/**
 * Main entry point for the Taxable Amount Calculator.
 * Calculates taxable amounts from E-Trade RSU benefit history CSV exports.
 */
public class TaxableAmountCalculator {

    static {
        // Configure logging
        try (InputStream is = TaxableAmountCalculator.class.getResourceAsStream("/logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (IOException e) {
            System.err.println("Could not load logging configuration: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        String restrictedStockCsv = args[0];
        String gainsLosesCsv = args[1];
        int taxYear;
        BigDecimal yearlyRate = null;

        try {
            taxYear = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid year format: " + args[2]);
            printUsage();
            System.exit(1);
            return;
        }

        // Optional: yearly average rate from command line
        if (args.length >= 4) {
            try {
                yearlyRate = new BigDecimal(args[3]);
                System.out.println("Using provided yearly average rate: " + yearlyRate + " CZK/USD");
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid yearly rate format: " + args[3]);
                printUsage();
                System.exit(1);
                return;
            }
        }

        try {
            run(Path.of(restrictedStockCsv), Path.of(gainsLosesCsv), taxYear, yearlyRate);
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java TaxableAmountCalculator <restricted-stock-csv> <gains-loses-csv> <tax-year> [yearly-rate]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  restricted-stock-csv  Path to E-Trade Restricted Stock (Grant History) CSV");
        System.out.println("  gains-loses-csv       Path to E-Trade Gains & Losses CSV file");
        System.out.println("  tax-year              Calendar year for tax calculation (e.g., 2025)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  ./gradlew run --args=\"'Restricted Stock.csv' Gains_Loses.csv 2025\"");
        System.out.println();
        System.out.println("Note: Historical GFŘ yearly rates (2019-2025) are now hardcoded in the application.");
        System.out.println("      Daily rates are fetched from CNB API for all years.");
        System.out.println("      Yearly rates: 2019=22.93, 2020=23.14, 2021=21.72, 2022=23.41,");
        System.out.println("                    2023=22.14, 2024=23.28, 2025=21.84");
    }

    public static void run(Path restrictedStockCsv, Path gainsLosesCsv, int taxYear, BigDecimal yearlyRate) throws IOException {
        // Setup exchange rate provider
        ExchangeRateProvider rateProvider = createExchangeRateProvider();

        // Parse and calculate VESTING (from Restricted Stock CSV)
        System.out.println("\n=== PARSING VESTING EVENTS ===");
        RestrictedStockCsvParser vestParser = new RestrictedStockCsvParser();
        List<VestEvent> vestEvents = vestParser.parse(restrictedStockCsv);
        VestingCalculator vestingCalculator = new VestingCalculator(rateProvider);
        VestingReport vestingReport = vestingCalculator.calculateForYear(vestEvents, taxYear);

        // Parse and calculate SELLING (from Gains & Losses CSV)
        System.out.println("\n=== PARSING SELL TRANSACTIONS ===");
        GainsLossesCsvParser sellParser = new GainsLossesCsvParser();
        List<StockTransaction> sellTransactions = sellParser.parse(gainsLosesCsv);
        RsuTaxCalculator sellCalculator = new RsuTaxCalculator(rateProvider);
        TaxReport sellReport = sellCalculator.calculateForYear(sellTransactions, taxYear);

        // Format and output
        ReportFormatter formatter = new ConsoleTableReportFormatter();
        ReportPrinter printer = new ConsolePrinter();

        String formattedReport = formatter.format(vestingReport, sellReport);
        printer.print(formattedReport);
    }

    private static ExchangeRateProvider createExchangeRateProvider() {
        System.out.println("Using historical GFŘ yearly rates (2019-2025) + CNB daily rates");
        System.out.println("Yearly rates: 2019=22.93, 2020=23.14, 2021=21.72, 2022=23.41, 2023=22.14, 2024=23.28, 2025=21.84");
        return new CombinedExchangeRateProvider(new CnbExchangeRateProvider());
    }
}

