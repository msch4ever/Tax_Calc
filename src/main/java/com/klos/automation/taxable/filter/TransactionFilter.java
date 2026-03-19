package com.klos.automation.taxable.filter;

import com.klos.automation.taxable.model.PlanType;
import com.klos.automation.taxable.model.StockTransaction;

import java.util.List;

/**
 * Utility class for filtering stock transactions.
 */
public final class TransactionFilter {
    
    private TransactionFilter() {
        // Utility class
    }
    
    /**
     * Filter transactions by plan type.
     *
     * @param transactions list of transactions
     * @param planType     the plan type to filter by
     * @return filtered list
     */
    public static List<StockTransaction> byPlanType(List<StockTransaction> transactions, PlanType planType) {
        return transactions.stream()
                .filter(t -> t.planType() == planType)
                .toList();
    }
    
    /**
     * Filter transactions to RSU only.
     *
     * @param transactions list of transactions
     * @return only RSU transactions
     */
    public static List<StockTransaction> rsuOnly(List<StockTransaction> transactions) {
        return byPlanType(transactions, PlanType.RS);
    }
    
    /**
     * Filter transactions to ESPP only.
     *
     * @param transactions list of transactions
     * @return only ESPP transactions
     */
    public static List<StockTransaction> esppOnly(List<StockTransaction> transactions) {
        return byPlanType(transactions, PlanType.ESPP);
    }
    
    /**
     * Filter transactions by sell year.
     *
     * @param transactions list of transactions
     * @param year         the calendar year to filter by
     * @return transactions sold in the specified year
     */
    public static List<StockTransaction> bySellYear(List<StockTransaction> transactions, int year) {
        return transactions.stream()
                .filter(t -> t.sellYear() == year)
                .toList();
    }
    
    /**
     * Filter transactions by vest year.
     *
     * @param transactions list of transactions
     * @param year         the calendar year to filter by
     * @return transactions vested in the specified year
     */
    public static List<StockTransaction> byVestYear(List<StockTransaction> transactions, int year) {
        return transactions.stream()
                .filter(t -> t.vestYear() == year)
                .toList();
    }
    
    /**
     * Filter RSU transactions for a specific tax year.
     * For tax purposes, we typically care about the sell date year.
     *
     * @param transactions list of transactions
     * @param year         the calendar year
     * @return RSU transactions sold in the specified year
     */
    public static List<StockTransaction> rsuForTaxYear(List<StockTransaction> transactions, int year) {
        return transactions.stream()
                .filter(StockTransaction::isRsu)
                .filter(t -> t.sellYear() == year)
                .toList();
    }
}

