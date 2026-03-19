package com.klos.automation.taxable.calculator;

import com.klos.automation.taxable.exchange.ManualExchangeRateProvider;
import com.klos.automation.taxable.filter.TransactionFilter;
import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;
import com.klos.automation.taxable.model.TaxReport;
import com.klos.automation.taxable.parser.GainsLossesCsvParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RsuTaxCalculatorTest {
    
    private RsuTaxCalculator calculator;
    private ManualExchangeRateProvider rateProvider;
    
    @BeforeEach
    void setUp() {
        rateProvider = new ManualExchangeRateProvider();
        rateProvider.setYearlyAverageRate(2025, "23.50");
        rateProvider.setDefaultRate("23.50");

        // Set specific rates for test data dates (vest dates and sell dates)
        rateProvider.setRate(LocalDate.of(2025, 2, 20), "23.20");
        rateProvider.setRate(LocalDate.of(2025, 3, 15), "23.30");
        rateProvider.setRate(LocalDate.of(2025, 6, 20), "23.40");
        rateProvider.setRate(LocalDate.of(2025, 6, 23), "23.45");
        rateProvider.setRate(LocalDate.of(2025, 9, 20), "23.60");
        rateProvider.setRate(LocalDate.of(2025, 9, 23), "23.65");
        rateProvider.setRate(LocalDate.of(2022, 3, 1), "22.00");

        calculator = new RsuTaxCalculator(rateProvider);
    }

    @Test
    void shouldCalculateReportForYear2025() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        assertThat(report.taxYear()).isEqualTo(2025);
        assertThat(report.planType()).isEqualTo(PlanType.RS);
        assertThat(report.totalTransactions()).isEqualTo(4);
    }

    @Test
    void shouldCalculateTotalVestIncomeUsd() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        // Sum of all Ordinary Income Recognized for RSU: $2500 + $5000 + $1500 + $1600 = $10,600
        assertThat(report.totalVestIncomeUsd()).isEqualByComparingTo(new BigDecimal("10600.00"));
    }

    @Test
    void shouldCalculateTotalCapitalGainUsd() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        // Sum of all Adjusted Gain/Loss for RSU: $131.50 + $300 + $60 + $400 = $891.50
        assertThat(report.totalCapitalGainLossUsd()).isEqualByComparingTo(new BigDecimal("891.50"));
    }

    @Test
    void shouldIdentifyExemptTransactions() throws IOException {
        // One transaction (RSU-004) is long-term (acquired 2022, held > 3 years)
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        assertThat(report.exemptTransactions()).isEqualTo(1);
        // Taxable = Total - Exempt ($891.50 - $400 = $491.50)
        assertThat(report.totalTaxableCapitalGainUsd()).isEqualByComparingTo(new BigDecimal("491.50"));
    }

    @Test
    void shouldConvertToCzkUsingTransactionDateRates() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        // Vest income should be converted using vest date rates
        assertThat(report.totalVestIncomeCzkByDate()).isGreaterThan(BigDecimal.ZERO);
        // Should not equal yearly average conversion (different rates)
        assertThat(report.totalVestIncomeCzkByDate())
                .isNotEqualByComparingTo(report.totalVestIncomeCzkByYearlyAvg());
    }

    @Test
    void shouldConvertToCzkUsingYearlyAverageRate() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        TaxReport report = calculator.calculateForYear(transactions, 2025);

        // With yearly average of 23.50, vest income should be: 10600 * 23.50 = 249,100
        BigDecimal expected = new BigDecimal("10600.00").multiply(new BigDecimal("23.50"));
        assertThat(report.totalVestIncomeCzkByYearlyAvg()).isEqualByComparingTo(expected);
    }
    
    private List<StockTransaction> parseTestFile() throws IOException {
        GainsLossesCsvParser parser = new GainsLossesCsvParser();
        InputStream is = getClass().getResourceAsStream("/Gains_Loses.csv");
        assertThat(is).isNotNull();
        List<StockTransaction> all = parser.parse(new InputStreamReader(is));
        // Filter to RSU only - calculator expects pre-filtered transactions
        return TransactionFilter.rsuOnly(all);
    }
}

