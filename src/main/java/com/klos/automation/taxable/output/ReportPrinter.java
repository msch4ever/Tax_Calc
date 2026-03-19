package com.klos.automation.taxable.output;

/**
 * Outputs/prints formatted report content.
 * Single responsibility: send formatted content to an output destination.
 */
public interface ReportPrinter {
    
    /**
     * Print the formatted content.
     *
     * @param content the formatted content to print
     */
    void print(String content);
    
    /**
     * Get the name of this printer.
     *
     * @return printer name
     */
    String getName();
}

