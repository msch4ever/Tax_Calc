package com.klos.automation.taxable.parser;

import com.klos.automation.taxable.model.VestEvent;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for E-Trade Restricted Stock (Grant History) CSV files.
 * Extracts vesting events with their taxable gain amounts.
 */
public class RestrictedStockCsvParser {
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    
    // Column indices
    private static final int COL_RECORD_TYPE = 0;
    private static final int COL_GRANT_NUMBER = 10;
    private static final int COL_DATE = 21;
    private static final int COL_EVENT_TYPE = 22;
    private static final int COL_QTY_OR_AMOUNT = 23;
    private static final int COL_VEST_PERIOD = 24;
    private static final int COL_VEST_DATE = 25;
    private static final int COL_VESTED_QTY = 32;
    private static final int COL_TAX_DESCRIPTION = 38;
    private static final int COL_TAXABLE_GAIN = 39;
    
    /**
     * Parse the Restricted Stock CSV from a Path and return all vest events.
     */
    public List<VestEvent> parse(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath)) {
            return parse(reader);
        }
    }

    /**
     * Parse the Restricted Stock CSV from an InputStream and return all vest events.
     */
    public List<VestEvent> parse(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream)) {
            return parse(reader);
        }
    }

    /**
     * Parse the Restricted Stock CSV from a Reader and return all vest events.
     */
    public List<VestEvent> parse(Reader inputReader) throws IOException {
        List<VestEvent> vestEvents = new ArrayList<>();

        // First pass: collect vest schedule info (vest period -> vest date, quantity)
        // Second pass: match with Tax Withholding rows to get taxable gain
        Map<String, VestScheduleInfo> vestSchedules = new HashMap<>();
        Map<String, BigDecimal> taxWithholdings = new HashMap<>();

        try (CSVReader reader = new CSVReader(inputReader)) {
            List<String[]> rows = reader.readAll();
            
            // Skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String recordType = safeGet(row, COL_RECORD_TYPE);
                String grantNumber = safeGet(row, COL_GRANT_NUMBER);
                
                if ("Vest Schedule".equals(recordType)) {
                    parseVestSchedule(row, grantNumber, vestSchedules);
                } else if ("Tax Withholding".equals(recordType)) {
                    parseTaxWithholding(row, grantNumber, taxWithholdings);
                }
            }
        } catch (CsvException e) {
            throw new IOException("Error parsing CSV: " + e.getMessage(), e);
        }
        
        // Match vest schedules with tax withholdings
        for (Map.Entry<String, VestScheduleInfo> entry : vestSchedules.entrySet()) {
            String key = entry.getKey();
            VestScheduleInfo schedule = entry.getValue();
            BigDecimal taxableGain = taxWithholdings.getOrDefault(key, BigDecimal.ZERO);
            
            // Only include if there's actual vested quantity and taxable gain
            if (schedule.vestedQty > 0 && taxableGain.compareTo(BigDecimal.ZERO) > 0) {
                vestEvents.add(VestEvent.of(
                        schedule.grantNumber,
                        schedule.vestDate,
                        schedule.vestedQty,
                        taxableGain
                ));
            }
        }
        
        return vestEvents;
    }
    
    private void parseVestSchedule(String[] row, String grantNumber, Map<String, VestScheduleInfo> vestSchedules) {
        String vestPeriod = safeGet(row, COL_VEST_PERIOD);
        String vestDateStr = safeGet(row, COL_VEST_DATE);
        String vestedQtyStr = safeGet(row, COL_VESTED_QTY);
        
        if (!vestDateStr.isEmpty() && !vestedQtyStr.isEmpty()) {
            LocalDate vestDate = parseDate(vestDateStr);
            int vestedQty = parseInt(vestedQtyStr);
            
            String key = grantNumber + "_" + vestPeriod;
            vestSchedules.put(key, new VestScheduleInfo(grantNumber, vestDate, vestedQty));
        }
    }
    
    private void parseTaxWithholding(String[] row, String grantNumber, Map<String, BigDecimal> taxWithholdings) {
        String vestPeriod = safeGet(row, COL_VEST_PERIOD);
        String taxableGainStr = safeGet(row, COL_TAXABLE_GAIN);
        
        if (!taxableGainStr.isEmpty()) {
            BigDecimal taxableGain = parseMoney(taxableGainStr);
            String key = grantNumber + "_" + vestPeriod;
            taxWithholdings.put(key, taxableGain);
        }
    }
    
    private String safeGet(String[] row, int index) {
        return index < row.length ? row[index].trim() : "";
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
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
    
    private record VestScheduleInfo(String grantNumber, LocalDate vestDate, int vestedQty) {}
}

