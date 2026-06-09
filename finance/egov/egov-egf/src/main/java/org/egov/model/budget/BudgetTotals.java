package org.egov.model.budget;

import java.math.BigDecimal;


/**
 * BudgetTotals class
 *
 * This is a Data Transfer Object (DTO) class used for aggregating and displaying
 * total budget amounts across multiple budget items. It provides a consolidated
 * view of budget estimates, actuals, and revised figures for reporting and
 * analysis purposes.
 *
 * Key Features:
 * - Aggregates budget totals across different financial periods
 * - Provides immutable totals through constructor initialization
 * - Initializes all values to ZERO to prevent null pointer exceptions
 * - Used primarily for budget reports and summary displays
 *
 * Usage:
 * This class is typically used when:
 * - Generating budget summary reports
 * - Displaying department-wise or function-wise budget totals
 * - Comparing budget vs actual across financial years
 * - Dashboard displays of budget information
 *
 * Not a JPA entity - purely for data aggregation and display
 */
public class BudgetTotals {
    private BigDecimal estimate = BigDecimal.ZERO;
    private BigDecimal actual = BigDecimal.ZERO;
    private BigDecimal revised = BigDecimal.ZERO;
    private BigDecimal next = BigDecimal.ZERO;

    public BudgetTotals(BigDecimal estimate, BigDecimal actual, BigDecimal revised, BigDecimal next) {
        this.estimate = estimate;
        this.actual = actual;
        this.revised = revised;
        this.next = next;
    }

    public BigDecimal getEstimate() { return estimate; }
    public BigDecimal getActual() { return actual; }
    public BigDecimal getRevised() { return revised; }
    public BigDecimal getNext() { return next; }
}
