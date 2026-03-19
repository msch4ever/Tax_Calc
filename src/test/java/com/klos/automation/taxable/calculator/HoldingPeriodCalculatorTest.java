package com.klos.automation.taxable.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;

class HoldingPeriodCalculatorTest {
    
    private HoldingPeriodCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new HoldingPeriodCalculator();
    }
    
    @Test
    void shouldCalculateShortHoldingPeriod() {
        LocalDate vestDate = LocalDate.of(2025, 9, 20);
        LocalDate sellDate = LocalDate.of(2025, 9, 23);
        
        Period period = calculator.calculateHoldingPeriod(vestDate, sellDate);
        
        assertThat(period.getYears()).isZero();
        assertThat(period.getMonths()).isZero();
        assertThat(period.getDays()).isEqualTo(3);
    }
    
    @Test
    void shouldCalculateMultiYearHoldingPeriod() {
        LocalDate vestDate = LocalDate.of(2022, 1, 15);
        LocalDate sellDate = LocalDate.of(2025, 6, 20);
        
        Period period = calculator.calculateHoldingPeriod(vestDate, sellDate);
        
        assertThat(period.getYears()).isEqualTo(3);
        assertThat(period.getMonths()).isEqualTo(5);
        assertThat(period.getDays()).isEqualTo(5);
    }
    
    @Test
    void shouldNotBeExemptIfHeldLessThan3Years() {
        LocalDate vestDate = LocalDate.of(2023, 1, 1);
        LocalDate sellDate = LocalDate.of(2025, 6, 1);  // 2 years, 5 months
        
        assertThat(calculator.isCapitalGainsExempt(vestDate, sellDate)).isFalse();
    }
    
    @Test
    void shouldNotBeExemptIfHeldExactly3Years() {
        LocalDate vestDate = LocalDate.of(2022, 1, 1);
        LocalDate sellDate = LocalDate.of(2025, 1, 1);  // Exactly 3 years
        
        assertThat(calculator.isCapitalGainsExempt(vestDate, sellDate)).isFalse();
    }
    
    @Test
    void shouldBeExemptIfHeldMoreThan3Years() {
        LocalDate vestDate = LocalDate.of(2022, 1, 1);
        LocalDate sellDate = LocalDate.of(2025, 1, 2);  // 3 years + 1 day
        
        assertThat(calculator.isCapitalGainsExempt(vestDate, sellDate)).isTrue();
    }
    
    @Test
    void shouldBeExemptIfHeld4Years() {
        LocalDate vestDate = LocalDate.of(2021, 1, 1);
        LocalDate sellDate = LocalDate.of(2025, 1, 1);  // 4 years
        
        assertThat(calculator.isCapitalGainsExempt(vestDate, sellDate)).isTrue();
    }
    
    @Test
    void shouldFormatShortPeriod() {
        Period period = Period.of(0, 0, 3);
        
        assertThat(calculator.formatHoldingPeriod(period)).isEqualTo("3d");
    }
    
    @Test
    void shouldFormatLongPeriod() {
        Period period = Period.of(3, 5, 15);
        
        assertThat(calculator.formatHoldingPeriod(period)).isEqualTo("3y 5m 15d");
    }
    
    @Test
    void shouldFormatPeriodWithZeroMonths() {
        Period period = Period.of(2, 0, 10);
        
        assertThat(calculator.formatHoldingPeriod(period)).isEqualTo("2y 10d");
    }
}

