package com.klos.automation.taxable.model;

/**
 * Stock plan types from E-Trade exports.
 */
public enum PlanType {
    RS,     // Restricted Stock (RSU)
    ESPP;   // Employee Stock Purchase Plan
    
    /**
     * Parse plan type from CSV string value.
     * 
     * @param value the CSV value (e.g., "RS", "ESPP")
     * @return the corresponding PlanType
     * @throws IllegalArgumentException if value is not recognized
     */
    public static PlanType fromCsvValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Plan type cannot be null or blank");
        }
        return switch (value.trim().toUpperCase()) {
            case "RS" -> RS;
            case "ESPP" -> ESPP;
            default -> throw new IllegalArgumentException("Unknown plan type: " + value);
        };
    }
}

