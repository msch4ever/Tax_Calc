package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.filter.TransactionFilter;
import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GainsLossesCsvParserTest {
    
    private GainsLossesCsvParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new GainsLossesCsvParser();
    }
    
    @Test
    void shouldParseAllSellTransactions() throws IOException {
        List<StockTransaction> transactions = parseTestFile();

        // 5 Sell rows in the test file (1 ESPP + 4 RSU), excludes Summary row
        assertThat(transactions).hasSize(5);
        assertThat(transactions).allMatch(t -> "Sell".equals(t.recordType()));
    }

    @Test
    void shouldParseRsuTransactions() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> rsuTransactions = TransactionFilter.rsuOnly(transactions);

        assertThat(rsuTransactions).hasSize(4);
        assertThat(rsuTransactions).allMatch(StockTransaction::isRsu);
    }

    @Test
    void shouldParseEsppTransactions() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> esppTransactions = TransactionFilter.esppOnly(transactions);

        assertThat(esppTransactions).hasSize(1);
        assertThat(esppTransactions).allMatch(StockTransaction::isEspp);
    }

    @Test
    void shouldParseTransactionDetails() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> rsuTransactions = TransactionFilter.rsuOnly(transactions);

        // Find the RSU transaction with 100 shares
        StockTransaction tx = rsuTransactions.stream()
                .filter(t -> t.quantity() == 100)
                .findFirst()
                .orElseThrow();

        assertThat(tx.symbol()).isEqualTo("ACME");
        assertThat(tx.planType()).isEqualTo(PlanType.RS);
        assertThat(tx.dateAcquired()).isEqualTo(LocalDate.of(2025, 6, 20));
        assertThat(tx.dateSold()).isEqualTo(LocalDate.of(2025, 6, 23));
        assertThat(tx.ordinaryIncomeRecognized()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(tx.gainLoss()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(tx.grantNumber()).isEqualTo("RSU-002");
    }

    @Test
    void shouldFilterBySellYear() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> year2025 = TransactionFilter.bySellYear(transactions, 2025);

        // All transactions in test file are sold in 2025
        assertThat(year2025).hasSize(5);
    }

    @Test
    void shouldFilterRsuForTaxYear() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> rsu2025 = TransactionFilter.rsuForTaxYear(transactions, 2025);

        assertThat(rsu2025).hasSize(4);
        assertThat(rsu2025).allMatch(StockTransaction::isRsu);
        assertThat(rsu2025).allMatch(t -> t.sellYear() == 2025);
    }

    @Test
    void shouldCalculateTotalOrdinaryIncome() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> rsuTransactions = TransactionFilter.rsuOnly(transactions);

        BigDecimal totalOrdinaryIncome = rsuTransactions.stream()
                .map(StockTransaction::ordinaryIncomeRecognized)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // $2500 + $5000 + $1500 + $1600 = $10,600
        assertThat(totalOrdinaryIncome).isEqualByComparingTo(new BigDecimal("10600.00"));
    }

    @Test
    void shouldCalculateTotalGainLoss() throws IOException {
        List<StockTransaction> transactions = parseTestFile();
        List<StockTransaction> rsuTransactions = TransactionFilter.rsuOnly(transactions);

        BigDecimal totalGainLoss = rsuTransactions.stream()
                .map(StockTransaction::gainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // $131.50 + $300.00 + $60.00 + $400.00 = $891.50
        assertThat(totalGainLoss).isEqualByComparingTo(new BigDecimal("891.50"));
    }
    
    private List<StockTransaction> parseTestFile() throws IOException {
        InputStream is = getClass().getResourceAsStream("/Gains_Loses.csv");
        assertThat(is).isNotNull();
        return parser.parse(new InputStreamReader(is));
    }
}

