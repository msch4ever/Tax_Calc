package com.klos.automation.taxable.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Debug test to understand ESPP sheet structure.
 * Only runs if the source file exists.
 */
class EsppSheetDebugTest {

    @Test
    void dumpEsppSheetStructure() throws Exception {
        Path sourcePath = Path.of("sources/BenefitHistory.xlsx");
        if (!Files.exists(sourcePath)) {
            System.out.println("Source file not found, skipping test");
            return;
        }

        var converter = new ExcelToCsvConverter();
        try (var fis = new FileInputStream(sourcePath.toFile())) {
            var csv = converter.convertToCsv(fis, "ESPP");
            var reader = new BufferedReader(new InputStreamReader(csv));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 10) {
                System.out.println("ROW " + count + ": " + line);
                // Also print column indices for header row
                if (count == 0) {
                    String[] cols = line.split(",");
                    System.out.println("\n=== COLUMN INDICES ===");
                    for (int i = 0; i < cols.length; i++) {
                        System.out.println("  " + i + ": " + cols[i]);
                    }
                    System.out.println("======================\n");
                }
                count++;
            }
        }
    }
}

