package com.klos.automation.taxable.output;

import java.io.PrintStream;

/**
 * Prints content to console (System.out or specified PrintStream).
 * Single responsibility: output content to console.
 */
public class ConsolePrinter implements ReportPrinter {
    
    private final PrintStream out;
    
    public ConsolePrinter() {
        this(System.out);
    }
    
    public ConsolePrinter(PrintStream out) {
        this.out = out;
    }
    
    @Override
    public void print(String content) {
        out.print(content);
    }
    
    @Override
    public String getName() {
        return "Console Printer";
    }
}

