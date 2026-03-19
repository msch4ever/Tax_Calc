package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.model.EsppPurchaseEvent;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parser for E-Trade ESPP (Employee Stock Purchase Plan) CSV/Excel files.
 * Extracts purchase events with their discount benefit amounts.
 * 
 * ESPP taxable income = (Purchase Date FMV - Purchase Price) × Quantity
 */
public class EsppPurchaseParser {
    
    // Case-insensitive date formatter for "15-MAR-2025" or "15-Mar-2025"
    private static final DateTimeFormatter DATE_FMT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yyyy")
            .toFormatter(Locale.ENGLISH);
    
    // Column indices from ESPP sheet
    private static final int COL_RECORD_TYPE = 0;
    private static final int COL_PURCHASE_DATE = 2;
    private static final int COL_PURCHASE_PRICE = 3;
    private static final int COL_PURCHASED_QTY = 4;
    private static final int COL_GRANT_DATE = 9;
    private static final int COL_DISCOUNT_PERCENT = 10;
    private static final int COL_PURCHASE_DATE_FMV = 12;
    
    /**
     * Parse the ESPP CSV from a Path and return all purchase events.
     */
    public List<EsppPurchaseEvent> parse(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return parse(reader);
        }
    }

    /**
     * Parse the ESPP CSV from an InputStream and return all purchase events.
     */
    public List<EsppPurchaseEvent> parse(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream)) {
            return parse(reader);
        }
    }

    /**
     * Parse the ESPP CSV from a Reader and return all purchase events.
     */
    public List<EsppPurchaseEvent> parse(Reader inputReader) throws IOException {
        List<EsppPurchaseEvent> purchases = new ArrayList<>();

        try (CSVReader reader = new CSVReader(inputReader)) {
            List<String[]> rows = reader.readAll();
            
            // Skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String recordType = safeGet(row, COL_RECORD_TYPE);
                
                // Only process Purchase records
                if ("Purchase".equalsIgnoreCase(recordType)) {
                    EsppPurchaseEvent purchase = parsePurchaseRow(row);
                    if (purchase != null) {
                        purchases.add(purchase);
                    }
                }
            }
        } catch (CsvException e) {
            throw new IOException("Error parsing ESPP CSV: " + e.getMessage(), e);
        }
        
        return purchases;
    }
    
    private EsppPurchaseEvent parsePurchaseRow(String[] row) {
        String purchaseDateStr = safeGet(row, COL_PURCHASE_DATE);
        String purchasePriceStr = safeGet(row, COL_PURCHASE_PRICE);
        String quantityStr = safeGet(row, COL_PURCHASED_QTY);
        String purchaseFmvStr = safeGet(row, COL_PURCHASE_DATE_FMV);
        String grantDateStr = safeGet(row, COL_GRANT_DATE);
        
        if (purchaseDateStr.isEmpty() || purchasePriceStr.isEmpty() || 
            quantityStr.isEmpty() || purchaseFmvStr.isEmpty()) {
            return null;
        }
        
        LocalDate purchaseDate = parseDate(purchaseDateStr);
        BigDecimal purchasePrice = parseMoney(purchasePriceStr);
        int quantity = parseInt(quantityStr);
        BigDecimal purchaseFmv = parseMoney(purchaseFmvStr);
        
        // Use grant date as a pseudo grant number (for grouping)
        String grantNumber = "ESPP-" + grantDateStr.replace("-", "");
        
        return EsppPurchaseEvent.of(grantNumber, purchaseDate, quantity, purchasePrice, purchaseFmv);
    }
    
    private String safeGet(String[] row, int index) {
        return index < row.length ? row[index].trim() : "";
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        // Case-insensitive parsing of "15-MAR-2025" or "15-Mar-2025"
        return LocalDate.parse(dateStr, DATE_FMT);
    }
    
    private int parseInt(String str) {
        if (str == null || str.isEmpty()) return 0;
        try {
            return Integer.parseInt(str.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private BigDecimal parseMoney(String str) {
        if (str == null || str.isEmpty()) return BigDecimal.ZERO;
        String cleaned = str.replaceAll("[^0-9.-]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}

