package com.klos.automation.taxable.web;

import com.klos.automation.taxable.model.TaxableEsppPurchase;
import com.klos.automation.taxable.model.TaxableTransaction;
import com.klos.automation.taxable.model.TaxableVestEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
public class TaxCalculatorController {

    private final TaxCalculationService calculationService;

    public TaxCalculatorController(TaxCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Default to previous year (e.g., 2025 when in 2026) - most common use case for tax filing
        model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
        return "index";
    }

    @GetMapping("/tax-guide")
    public String taxGuide() {
        return "tax-guide";
    }

    @PostMapping("/calculate")
    public String calculate(
            @RequestParam("benefitHistoryFile") MultipartFile benefitHistoryFile,
            @RequestParam("gainsLossesFile") MultipartFile gainsLossesFile,
            @RequestParam("taxYear") int taxYear,
            @RequestParam(value = "yearlyRate", required = false) String yearlyRateStr,
            @RequestParam(value = "processRsu", required = false) boolean processRsu,
            @RequestParam(value = "processEspp", required = false) boolean processEspp,
            Model model) {

        try {
            // Validate at least one plan type selected
            if (!processRsu && !processEspp) {
                model.addAttribute("error", "Please select at least one plan type (RSU or ESPP)");
                model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
                return "index";
            }

            BigDecimal yearlyRate = null;
            if (yearlyRateStr != null && !yearlyRateStr.isBlank()) {
                yearlyRate = new BigDecimal(yearlyRateStr.trim());
            }

            TaxCalculationResult result = calculationService.calculate(
                    benefitHistoryFile.getInputStream(),
                    benefitHistoryFile.getOriginalFilename(),
                    gainsLossesFile.getInputStream(),
                    gainsLossesFile.getOriginalFilename(),
                    taxYear,
                    yearlyRate,
                    processRsu,
                    processEspp
            );

            // Build exchange rate reference table
            Map<LocalDate, BigDecimal> exchangeRates = buildExchangeRateTable(result);

            // Calculate recommendation (grand total across RSU + ESPP)
            RateRecommendation recommendation = calculateRecommendation(result);

            // RSU reports
            model.addAttribute("rsuVestingReport", result.rsuVestingReport());
            model.addAttribute("rsuSellReport", result.rsuSellReport());

            // ESPP reports
            model.addAttribute("esppPurchaseReport", result.esppPurchaseReport());
            model.addAttribute("esppSellReport", result.esppSellReport());

            // Flags for conditional display in template
            model.addAttribute("processRsu", processRsu);
            model.addAttribute("processEspp", processEspp);

            model.addAttribute("logMessages", result.logMessages());
            model.addAttribute("exchangeRates", exchangeRates);
            model.addAttribute("taxYear", taxYear);
            model.addAttribute("recommendation", recommendation);

            return "results";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error processing files: " + e.getMessage());
            model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
            return "index";
        }
    }
    
    private Map<LocalDate, BigDecimal> buildExchangeRateTable(TaxCalculationResult result) {
        Map<LocalDate, BigDecimal> rates = new TreeMap<>();

        // RSU vesting events
        for (TaxableVestEvent v : result.rsuVestingReport().vestEvents()) {
            rates.put(v.vestDate(), v.vestDateRate());
        }
        // RSU sell transactions
        for (TaxableTransaction t : result.rsuSellReport().transactions()) {
            rates.put(t.vestDate(), t.vestDateRate());
            rates.put(t.sellDate(), t.sellDateRate());
        }
        // ESPP purchase events
        for (TaxableEsppPurchase p : result.esppPurchaseReport().purchases()) {
            rates.put(p.purchaseDate(), p.purchaseDateRate());
        }
        // ESPP sell transactions
        for (TaxableTransaction t : result.esppSellReport().transactions()) {
            rates.put(t.vestDate(), t.vestDateRate());
            rates.put(t.sellDate(), t.sellDateRate());
        }

        return rates;
    }

    private RateRecommendation calculateRecommendation(TaxCalculationResult result) {
        // RSU totals
        BigDecimal rsuIncomeDaily = result.rsuVestingReport().totalTaxableGainCzkByDate();
        BigDecimal rsuIncomeYearly = result.rsuVestingReport().totalTaxableGainCzkByYearlyAvg();
        BigDecimal rsuGainsDaily = maxZero(result.rsuSellReport().totalTaxableCapitalGainCzkByDate());
        BigDecimal rsuGainsYearly = maxZero(result.rsuSellReport().totalTaxableCapitalGainCzkByYearlyAvg());
        BigDecimal rsuTotalDaily = rsuIncomeDaily.add(rsuGainsDaily);
        BigDecimal rsuTotalYearly = rsuIncomeYearly.add(rsuGainsYearly);

        // ESPP totals
        BigDecimal esppIncomeDaily = result.esppPurchaseReport().totalDiscountBenefitCzkByDate();
        BigDecimal esppIncomeYearly = result.esppPurchaseReport().totalDiscountBenefitCzkByYearlyAvg();
        BigDecimal esppGainsDaily = maxZero(result.esppSellReport().totalTaxableCapitalGainCzkByDate());
        BigDecimal esppGainsYearly = maxZero(result.esppSellReport().totalTaxableCapitalGainCzkByYearlyAvg());
        BigDecimal esppTotalDaily = esppIncomeDaily.add(esppGainsDaily);
        BigDecimal esppTotalYearly = esppIncomeYearly.add(esppGainsYearly);

        // Grand totals
        BigDecimal grandTotalDaily = rsuTotalDaily.add(esppTotalDaily);
        BigDecimal grandTotalYearly = rsuTotalYearly.add(esppTotalYearly);
        BigDecimal totalDiff = grandTotalDaily.subtract(grandTotalYearly);

        String betterRate = totalDiff.compareTo(BigDecimal.ZERO) > 0 ? "yearly" : "daily";
        BigDecimal savings = totalDiff.abs();

        return new RateRecommendation(
                rsuTotalDaily, rsuTotalYearly,
                esppTotalDaily, esppTotalYearly,
                grandTotalDaily, grandTotalYearly, totalDiff,
                betterRate, savings
        );
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    public record RateRecommendation(
            BigDecimal rsuTotalDaily, BigDecimal rsuTotalYearly,
            BigDecimal esppTotalDaily, BigDecimal esppTotalYearly,
            BigDecimal grandTotalDaily, BigDecimal grandTotalYearly, BigDecimal totalDiff,
            String betterRate, BigDecimal savings
    ) {}
}

