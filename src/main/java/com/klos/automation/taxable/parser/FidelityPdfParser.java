package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Fidelity NetBenefits "Custom transaction summary" PDF.
 * Extracts stock sale transactions from the "Stock sales" section.
 *
 * <p>Only RSU (RS) transactions are supported. The PDF provides:
 * Date sold, Date acquired, Quantity, Cost basis, Proceeds, Gain/loss, Stock source.
 *
 * <p>From these we can derive:
 * <ul>
 *   <li>FMV at vest (cost basis / quantity)</li>
 *   <li>Proceeds per share (proceeds / quantity)</li>
 * </ul>
 */
public class FidelityPdfParser {

    private static final Logger LOG = Logger.getLogger(FidelityPdfParser.class.getName());

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM-dd-yyyy", Locale.ENGLISH);

    // Pattern to match stock sale rows like:
    // Feb-08-2023                  Feb-05-2023           25.0000             $2,240.50           $2,171.83              -$68.67 USD            RS
    private static final Pattern SALE_ROW_PATTERN = Pattern.compile(
            "([A-Z][a-z]{2}-\\d{2}-\\d{4})\\s+" +    // Date sold or transferred
            "([A-Z][a-z]{2}-\\d{2}-\\d{4})\\s+" +    // Date acquired
            "([\\d,]+\\.\\d{4})\\s+" +                // Quantity
            "\\$([\\d,]+\\.\\d{2})\\s+" +             // Cost basis
            "\\$([\\d,]+\\.\\d{2})\\s+" +             // Proceeds
            "([+\\-])\\s*\\$([\\d,]+\\.\\d{2})\\s+USD\\s+" + // Gain/loss sign + amount
            "(RS|SP|SA|NQ|ISO|DO)"                     // Stock source
    );

    // Pattern to match stock ticker header like: ORCL: ORACLE CORP
    private static final Pattern TICKER_PATTERN = Pattern.compile(
            "^([A-Z]+):\\s+(.+)$", Pattern.MULTILINE
    );

    /**
     * Parse stock sale transactions from a Fidelity PDF input stream.
     *
     * @param inputStream the PDF file content
     * @return list of parsed StockTransaction records
     * @throws IOException if PDF cannot be read
     */
    public List<StockTransaction> parse(InputStream inputStream) throws IOException {
        byte[] pdfBytes = inputStream.readAllBytes();
        String text;

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
        }

        return parseText(text);
    }

    /**
     * Parse stock sale transactions from extracted PDF text.
     * Visible for testing.
     *
     * @param text the extracted text from the PDF
     * @return list of parsed StockTransaction records
     */
    List<StockTransaction> parseText(String text) {
        List<StockTransaction> transactions = new ArrayList<>();

        // Find the stock ticker symbol from the PDF
        String symbol = "UNKNOWN";
        Matcher tickerMatcher = TICKER_PATTERN.matcher(text);
        if (tickerMatcher.find()) {
            symbol = tickerMatcher.group(1);
        }

        // Find the "Stock sales" section
        int stockSalesIndex = text.indexOf("Stock sales");
        if (stockSalesIndex == -1) {
            LOG.warning("No 'Stock sales' section found in PDF");
            return transactions;
        }

        // Parse only from the "Stock sales" section onwards
        String salesSection = text.substring(stockSalesIndex);

        Matcher matcher = SALE_ROW_PATTERN.matcher(salesSection);
        int rowCount = 0;

        while (matcher.find()) {
            rowCount++;
            StockTransaction tx = parseMatch(matcher, symbol, rowCount);
            transactions.add(tx);
        }

        LOG.info(() -> String.format("Parsed %d stock sale transactions from Fidelity PDF", transactions.size()));
        return transactions;
    }

    private StockTransaction parseMatch(Matcher matcher, String symbol, int rowNumber) {
        LocalDate dateSold = parseDate(matcher.group(1));
        LocalDate dateAcquired = parseDate(matcher.group(2));
        BigDecimal quantity = parseBigDecimal(matcher.group(3));
        BigDecimal costBasis = parseBigDecimal(matcher.group(4));
        BigDecimal proceeds = parseBigDecimal(matcher.group(5));
        String gainSign = matcher.group(6);
        BigDecimal gainAmount = parseBigDecimal(matcher.group(7));
        String stockSource = matcher.group(8);

        // Apply sign to gain/loss
        BigDecimal gainLoss = "-".equals(gainSign) ? gainAmount.negate() : gainAmount;

        // Calculate per-share values
        int qty = quantity.intValue();
        BigDecimal fmvAtVest = qty > 0
                ? costBasis.divide(quantity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal proceedsPerShare = qty > 0
                ? proceeds.divide(quantity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Map stock source to PlanType (RS for RSU)
        PlanType planType = PlanType.RS;

        // Generate a synthetic grant number since Fidelity PDF doesn't provide one
        String grantNumber = String.format("FID-%s-%d", dateAcquired.format(DateTimeFormatter.BASIC_ISO_DATE), rowNumber);

        LOG.info(() -> String.format(
                "Row %d: SOLD %s | Acquired %s | %s shares | Cost $%,.2f | Proceeds $%,.2f | Gain $%,.2f | %s",
                rowNumber, dateSold, dateAcquired, quantity, costBasis.doubleValue(),
                proceeds.doubleValue(), gainLoss.doubleValue(), stockSource
        ));

        return new StockTransaction(
                "Sell",                    // recordType
                symbol,                    // symbol
                planType,                  // planType
                qty,                       // quantity
                dateAcquired,              // dateAcquired
                dateAcquired,              // vestDate (same as dateAcquired for Fidelity)
                dateSold,                  // dateSold
                costBasis,                 // ordinaryIncomeRecognized (= cost basis for RSU)
                proceeds,                  // totalProceeds
                gainLoss,                  // gainLoss
                proceedsPerShare,          // proceedsPerShare
                fmvAtVest,                 // vestDateFmv
                fmvAtVest,                 // acquisitionCostPerShare
                grantNumber                // grantNumber
        );
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value.trim(), DATE_FORMAT);
    }

    private BigDecimal parseBigDecimal(String value) {
        return new BigDecimal(value.replace(",", ""));
    }
}
