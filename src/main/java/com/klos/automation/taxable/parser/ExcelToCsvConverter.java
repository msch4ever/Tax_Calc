package com.klos.automation.taxable.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Iterator;

/**
 * Converts Excel (XLSX) files to CSV format for parsing.
 */
public class ExcelToCsvConverter {

    private static final String CSV_SEPARATOR = ",";

    /**
     * Convert an Excel InputStream to CSV InputStream.
     * Looks for a sheet named "Restricted Stock", otherwise uses the first sheet.
     *
     * @param excelInputStream the Excel file input stream
     * @return InputStream containing CSV data
     * @throws IOException if reading fails
     */
    public InputStream convertToCsv(InputStream excelInputStream) throws IOException {
        return convertToCsv(excelInputStream, (String) null);
    }

    /**
     * Convert an Excel InputStream to CSV InputStream, finding sheet by name.
     *
     * @param excelInputStream the Excel file input stream
     * @param preferredSheetName the sheet name to look for (case-insensitive), null for first sheet
     * @return InputStream containing CSV data
     * @throws IOException if reading fails
     */
    public InputStream convertToCsv(InputStream excelInputStream, String preferredSheetName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(excelInputStream)) {
            Sheet sheet = findSheet(workbook, preferredSheetName);
            return convertSheetToCsv(sheet);
        }
    }

    /**
     * Convert an Excel InputStream to CSV InputStream by sheet index.
     *
     * @param excelInputStream the Excel file input stream
     * @param sheetIndex the sheet index to convert (0-based)
     * @return InputStream containing CSV data
     * @throws IOException if reading fails
     */
    public InputStream convertToCsv(InputStream excelInputStream, int sheetIndex) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(excelInputStream)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            return convertSheetToCsv(sheet);
        }
    }

    /**
     * Find a sheet by name (case-insensitive), or fall back to first sheet.
     */
    private Sheet findSheet(Workbook workbook, String preferredSheetName) {
        if (preferredSheetName != null) {
            // Try exact match first
            Sheet sheet = workbook.getSheet(preferredSheetName);
            if (sheet != null) {
                return sheet;
            }
            // Try case-insensitive match
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).equalsIgnoreCase(preferredSheetName)) {
                    return workbook.getSheetAt(i);
                }
            }
        }
        // Fall back to first sheet
        return workbook.getSheetAt(0);
    }

    /**
     * Convert a sheet to CSV format.
     */
    private InputStream convertSheetToCsv(Sheet sheet) {
        StringWriter stringWriter = new StringWriter();
        DataFormatter formatter = new DataFormatter();

        for (Row row : sheet) {
            StringBuilder rowBuilder = new StringBuilder();
            int lastCellNum = row.getLastCellNum();

            for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String cellValue = formatCellValue(cell, formatter);

                // Escape CSV special characters
                if (cellValue.contains(",") || cellValue.contains("\"") || cellValue.contains("\n")) {
                    cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
                }

                if (cellNum > 0) {
                    rowBuilder.append(CSV_SEPARATOR);
                }
                rowBuilder.append(cellValue);
            }

            stringWriter.write(rowBuilder.toString());
            stringWriter.write("\n");
        }

        return new ByteArrayInputStream(stringWriter.toString().getBytes());
    }

    private String formatCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield formatter.formatCellValue(cell);
                } else {
                    // Avoid scientific notation for large numbers
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value) && value < Long.MAX_VALUE) {
                        yield String.valueOf((long) value);
                    }
                    yield formatter.formatCellValue(cell);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield formatter.formatCellValue(cell);
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }

    /**
     * Detect if the input stream is an Excel file based on content type or filename.
     */
    public static boolean isExcelFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    /**
     * Detect if the input stream is a CSV file.
     */
    public static boolean isCsvFile(String filename) {
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".csv");
    }
}

