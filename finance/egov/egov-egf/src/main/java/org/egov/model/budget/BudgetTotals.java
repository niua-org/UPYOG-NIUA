package org.egov.model.budget;

import java.math.BigDecimal;

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
