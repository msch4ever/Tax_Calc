package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Parser for E-Trade Gains & Losses CSV exports.
 */
public class GainsLossesCsvParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // CSV column indices (0-based)
    private static final int COL_RECORD_TYPE = 0;
    private static final int COL_SYMBOL = 1;
    private static final int COL_PLAN_TYPE = 2;
    private static final int COL_QUANTITY = 3;
    private static final int COL_DATE_ACQUIRED = 4;
    private static final int COL_ACQUISITION_COST_PER_SHARE = 7;  // Actual purchase price per share (for ESPP)
    private static final int COL_ORDINARY_INCOME = 8;
    private static final int COL_ADJUSTED_COST_BASIS_PER_SHARE = 11;  // FMV at vest for RSU, FMV at purchase for ESPP
    private static final int COL_DATE_SOLD = 12;
    private static final int COL_TOTAL_PROCEEDS = 13;  // Total sale proceeds
    private static final int COL_PROCEEDS_PER_SHARE = 14;
    private static final int COL_ADJUSTED_GAIN_LOSS = 18;  // Adjusted Gain/Loss (actual capital gain)
    private static final int COL_GRANT_DATE = 33;         // For ESPP: enrollment/grant date
    private static final int COL_GRANT_NUMBER = 39;
    private static final int COL_VEST_DATE = 41;

    // Date formatter for creating ESPP grant identifiers
    private static final DateTimeFormatter ESPP_GRANT_FORMAT = DateTimeFormatter.ofPattern("ddMMMyyyy", java.util.Locale.ENGLISH);
    
    /**
     * Parse all transactions from a CSV file.
     *
     * @param csvPath path to the CSV file
     * @return list of parsed transactions (excludes Summary rows)
     * @throws IOException if file cannot be read
     */
    public List<StockTransaction> parse(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return parse(reader);
        }
    }

    /**
     * Parse all transactions from an InputStream.
     *
     * @param inputStream the CSV content stream
     * @return list of parsed transactions (excludes Summary rows)
     * @throws IOException if reading fails
     */
    public List<StockTransaction> parse(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream)) {
            return parse(reader);
        }
    }

    /**
     * Parse all transactions from a Reader.
     *
     * @param reader the CSV content reader
     * @return list of parsed transactions (excludes Summary rows)
     * @throws IOException if reading fails
     */
    public List<StockTransaction> parse(Reader reader) throws IOException {
        try (CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> rows = csvReader.readAll();
            
            // Skip header row, filter out Summary rows
            return rows.stream()
                    .skip(1) // Skip header
                    .filter(row -> row.length > 0 && "Sell".equalsIgnoreCase(row[COL_RECORD_TYPE]))
                    .map(this::parseRow)
                    .toList();
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV", e);
        }
    }
    
    private StockTransaction parseRow(String[] row) {
        PlanType planType = PlanType.fromCsvValue(row[COL_PLAN_TYPE]);
        String grantNumber = parseGrantNumber(row, planType);

        return new StockTransaction(
                row[COL_RECORD_TYPE],
                row[COL_SYMBOL],
                planType,
                parseInteger(row[COL_QUANTITY]),
                parseDate(row[COL_DATE_ACQUIRED]),
                parseVestDate(row),
                parseDate(row[COL_DATE_SOLD]),
                parseMoney(row[COL_ORDINARY_INCOME]),
                parseMoney(row[COL_TOTAL_PROCEEDS]),
                parseMoney(row[COL_ADJUSTED_GAIN_LOSS]),
                parseMoney(row[COL_PROCEEDS_PER_SHARE]),
                parseMoney(row[COL_ADJUSTED_COST_BASIS_PER_SHARE]),  // FMV at vest/purchase date
                parseMoney(row[COL_ACQUISITION_COST_PER_SHARE]),     // Actual purchase price per share
                grantNumber
        );
    }

    private String parseGrantNumber(String[] row, PlanType planType) {
        String grantNumber = safeGet(row, COL_GRANT_NUMBER, "");

        // For ESPP, grant number is often "--" or empty. Derive from Grant Date instead.
        if (planType == PlanType.ESPP && (grantNumber.isEmpty() || "--".equals(grantNumber))) {
            String grantDateStr = safeGet(row, COL_GRANT_DATE, null);
            if (grantDateStr != null && grantDateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                LocalDate grantDate = parseDate(grantDateStr);
                // Format: "ESPP-16SEP2024"
                return "ESPP-" + grantDate.format(ESPP_GRANT_FORMAT).toUpperCase();
            }
            return "ESPP-Unknown";
        }

        return grantNumber;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim(), DATE_FORMAT);
    }

    private LocalDate parseVestDate(String[] row) {
        // Try Vest Date column first
        String vestDateValue = safeGet(row, COL_VEST_DATE, null);
        if (vestDateValue != null && vestDateValue.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return parseDate(vestDateValue);
        }
        // Fall back to Date Acquired
        return parseDate(row[COL_DATE_ACQUIRED]);
    }

    private String safeGet(String[] row, int index, String defaultValue) {
        if (row.length > index && row[index] != null && !row[index].isBlank()) {
            return row[index].trim();
        }
        return defaultValue;
    }
    
    private int parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.trim().replace(",", ""));
    }
    
    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        // Remove $, commas, and handle negative values like -$6.00
        String cleaned = value.trim()
                .replace("$", "")
                .replace(",", "")
                .replace("\"", "");
        if (cleaned.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned);
    }
}

