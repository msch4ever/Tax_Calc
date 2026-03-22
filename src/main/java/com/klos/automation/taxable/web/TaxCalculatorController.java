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
    private final FidelityCalculationService fidelityCalculationService;

    public TaxCalculatorController(TaxCalculationService calculationService,
                                   FidelityCalculationService fidelityCalculationService) {
        this.calculationService = calculationService;
        this.fidelityCalculationService = fidelityCalculationService;
    }

    // ==================== LANDING PAGE ====================

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    // ==================== E-TRADE FLOW ====================

    @GetMapping("/etrade")
    public String etradeIndex(Model model) {
        model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
        return "etrade";
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
                return "etrade";
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
            return "etrade";
        }
    }

    // ==================== FIDELITY FLOW ====================

    @GetMapping("/fidelity")
    public String fidelityIndex(Model model) {
        model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
        return "fidelity";
    }

    @PostMapping("/fidelity/calculate")
    public String fidelityCalculate(
            @RequestParam("transactionSummaryFile") MultipartFile transactionSummaryFile,
            @RequestParam("taxYear") int taxYear,
            Model model) {

        try {
            TaxCalculationResult result = fidelityCalculationService.calculate(
                    transactionSummaryFile.getInputStream(),
                    taxYear
            );

            // Build exchange rate reference table from vest events and sell transactions
            Map<LocalDate, BigDecimal> exchangeRates = new TreeMap<>();
            for (TaxableVestEvent v : result.rsuVestingReport().vestEvents()) {
                exchangeRates.put(v.vestDate(), v.vestDateRate());
            }
            for (TaxableTransaction t : result.rsuSellReport().transactions()) {
                exchangeRates.put(t.vestDate(), t.vestDateRate());
                exchangeRates.put(t.sellDate(), t.sellDateRate());
            }

            // Calculate recommendation (RSU vesting + sell for Fidelity)
            RateRecommendation recommendation = calculateRecommendation(result);

            model.addAttribute("rsuVestingReport", result.rsuVestingReport());
            model.addAttribute("rsuSellReport", result.rsuSellReport());
            model.addAttribute("logMessages", result.logMessages());
            model.addAttribute("exchangeRates", exchangeRates);
            model.addAttribute("taxYear", taxYear);
            model.addAttribute("recommendation", recommendation);

            return "fidelity-results";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error processing Fidelity PDF: " + e.getMessage());
            model.addAttribute("currentYear", LocalDate.now().getYear() - 1);
            return "fidelity";
        }
    }

    // ==================== TAX GUIDE ====================

    @GetMapping("/tax-guide")
    public String taxGuide() {
        return "tax-guide";
    }

    // ==================== HELPERS ====================

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
        BigDecimal taxableDifference = totalDiff.abs();
        BigDecimal taxSavings = taxableDifference.multiply(new BigDecimal("0.23"));

        return new RateRecommendation(
                rsuTotalDaily, rsuTotalYearly,
                esppTotalDaily, esppTotalYearly,
                grandTotalDaily, grandTotalYearly, totalDiff,
                betterRate, taxableDifference, taxSavings
        );
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    public record RateRecommendation(
            BigDecimal rsuTotalDaily, BigDecimal rsuTotalYearly,
            BigDecimal esppTotalDaily, BigDecimal esppTotalYearly,
            BigDecimal grandTotalDaily, BigDecimal grandTotalYearly, BigDecimal totalDiff,
            String betterRate, BigDecimal taxableDifference, BigDecimal taxSavings
    ) {}
}
