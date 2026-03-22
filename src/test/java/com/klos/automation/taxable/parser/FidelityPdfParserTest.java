package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FidelityPdfParserTest {

    private FidelityPdfParser parser;

    @BeforeEach
    void setUp() {
        parser = new FidelityPdfParser();
    }

    @Test
    void shouldParseAllStockSaleTransactions() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // 7 stock sale rows in the test PDF
        assertThat(transactions).hasSize(7);
        assertThat(transactions).allMatch(t -> "Sell".equals(t.recordType()));
    }

    @Test
    void shouldParseAllAsRsu() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        assertThat(transactions).allMatch(StockTransaction::isRsu);
        assertThat(transactions).allMatch(t -> t.planType() == PlanType.RS);
    }

    @Test
    void shouldParseSymbol() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        assertThat(transactions).allMatch(t -> "ORCL".equals(t.symbol()));
    }

    @Test
    void shouldParseFirstTransactionDetails() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // First row: Feb-08-2023, acquired Feb-05-2023, 25 shares, cost $2,240.50, proceeds $2,171.83, loss -$68.67
        StockTransaction first = transactions.get(0);

        assertThat(first.dateSold()).isEqualTo(LocalDate.of(2023, 2, 8));
        assertThat(first.dateAcquired()).isEqualTo(LocalDate.of(2023, 2, 5));
        assertThat(first.quantity()).isEqualTo(25);
        assertThat(first.ordinaryIncomeRecognized()).isEqualByComparingTo(new BigDecimal("2240.50"));
        assertThat(first.totalProceeds()).isEqualByComparingTo(new BigDecimal("2171.83"));
        assertThat(first.gainLoss()).isEqualByComparingTo(new BigDecimal("-68.67"));
    }

    @Test
    void shouldCalculateFmvAtVest() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // First row: cost $2,240.50 / 25 shares = $89.62 per share
        StockTransaction first = transactions.get(0);
        assertThat(first.vestDateFmv()).isEqualByComparingTo(new BigDecimal("89.62"));
    }

    @Test
    void shouldCalculateProceedsPerShare() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // First row: proceeds $2,171.83 / 25 shares = $86.8732 per share
        StockTransaction first = transactions.get(0);
        assertThat(first.proceedsPerShare()).isEqualByComparingTo(new BigDecimal("86.8732"));
    }

    @Test
    void shouldParseLargeQuantityTransaction() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // Row 7: Sep-22-2023, 80 shares, cost $9,021.60, proceeds $8,832.72, loss -$188.88
        StockTransaction last = transactions.get(6);

        assertThat(last.dateSold()).isEqualTo(LocalDate.of(2023, 9, 22));
        assertThat(last.dateAcquired()).isEqualTo(LocalDate.of(2023, 9, 20));
        assertThat(last.quantity()).isEqualTo(80);
        assertThat(last.ordinaryIncomeRecognized()).isEqualByComparingTo(new BigDecimal("9021.60"));
        assertThat(last.totalProceeds()).isEqualByComparingTo(new BigDecimal("8832.72"));
        assertThat(last.gainLoss()).isEqualByComparingTo(new BigDecimal("-188.88"));
    }

    @Test
    void shouldParsePositiveGainLoss() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // Row 2: Mar-06-2023, gain +$6.98
        StockTransaction second = transactions.get(1);
        assertThat(second.gainLoss()).isEqualByComparingTo(new BigDecimal("6.98"));
        assertThat(second.gainLoss().signum()).isGreaterThan(0);
    }

    @Test
    void shouldParseNegativeGainLoss() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        // Row 1: Feb-08-2023, loss -$68.67
        StockTransaction first = transactions.get(0);
        assertThat(first.gainLoss()).isEqualByComparingTo(new BigDecimal("-68.67"));
        assertThat(first.gainLoss().signum()).isLessThan(0);
    }

    @Test
    void shouldCalculateTotalProceeds() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        BigDecimal totalProceeds = transactions.stream()
                .map(StockTransaction::totalProceeds)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // $2,171.83 + $2,247.48 + $2,182.73 + $2,249.98 + $2,341.65 + $7,623.87 + $8,832.72 = $27,650.26
        assertThat(totalProceeds).isEqualByComparingTo(new BigDecimal("27650.26"));
    }

    @Test
    void shouldCalculateTotalGainLoss() throws IOException {
        List<StockTransaction> transactions = parseTestPdf();

        BigDecimal totalGainLoss = transactions.stream()
                .map(StockTransaction::gainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // -68.67 + 6.98 + (-57.77) + 9.48 + 101.15 + 68.28 + (-188.88) = -129.43
        assertThat(totalGainLoss).isEqualByComparingTo(new BigDecimal("-129.43"));
    }

    private List<StockTransaction> parseTestPdf() throws IOException {
        InputStream is = getClass().getResourceAsStream("/Fidelity_Transaction_Summary.pdf");
        assertThat(is).isNotNull();
        return parser.parse(is);
    }
}
