package com.klos.automation.taxable.output;

import com.klos.automation.taxable.model.TaxReport;
import com.klos.automation.taxable.model.VestingReport;

/**
 * Formats a tax report into a string representation.
 * Single responsibility: convert report data to formatted string.
 */
public interface ReportFormatter {

    /**
     * Format the combined vesting and selling reports into a string.
     *
     * @param vestingReport the vesting income report (from Restricted Stock CSV)
     * @param sellReport the selling/capital gains report (from Gains & Losses CSV)
     * @return the formatted string representation
     */
    String format(VestingReport vestingReport, TaxReport sellReport);

    /**
     * Get the name of this formatter.
     *
     * @return formatter name
     */
    String getName();
}

