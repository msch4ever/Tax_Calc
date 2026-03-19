package com.klos.automation.taxable.web;

import com.klos.automation.taxable.model.EsppPurchaseReport;
import com.klos.automation.taxable.model.TaxReport;
import com.klos.automation.taxable.model.VestingReport;

public record TaxCalculationResult(
        // RSU
        VestingReport rsuVestingReport,
        TaxReport rsuSellReport,

        // ESPP
        EsppPurchaseReport esppPurchaseReport,
        TaxReport esppSellReport,

        // Flags for what was processed
        boolean rsuProcessed,
        boolean esppProcessed,

        String logMessages
) {}

