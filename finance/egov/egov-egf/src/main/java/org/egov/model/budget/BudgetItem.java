package org.egov.model.budget;


import jdk.nashorn.internal.runtime.logging.Loggable;
import lombok.Getter;
import lombok.Setter;

import org.egov.commons.CFinancialYear;
import org.egov.commons.CFunction;
import org.egov.commons.Scheme;
import org.egov.infra.persistence.entity.AbstractAuditable;
import org.egov.infra.persistence.validator.annotation.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;



/**
 * BudgetItem entity class
 *
 * This entity represents individual budget line items in the financial system.
 * Each budget item captures the budget estimates and actuals for a specific
 * combination of function, budget head, and financial year. It maintains both
 * current and next year estimates along with actual expenditure data.
 *
 * Key Features:
 * - Links budget heads with functions (departments) and financial years
 * - Captures current year estimate, actual, and revised estimate
 * - Captures next year estimate for budget planning
 * - Supports scheme-based budgeting
 * - Groups budget items using budget groups
 * - Generates unique budget codes based on function and budget head
 * - Maintains association with budget register for workflow
 * - Provides safe getters to handle null values
 *
 * Budget Item Lifecycle:
 * 1. Created for a specific financial year (nextEstimate)
 * 2. Tracks current financial year data (currentEstimate, currentActual, currentRevisedEstimate)
 * 3. Associated with budget register for approval workflow
 *
 * Table: egf_budgetitem
 * Sequence: seq_egf_budgetitem
 *
 */
@Entity
@Table(name = BudgetItem.TABLE_NAME)
@Unique(id = "id", tableName = BudgetItem.TABLE_NAME, enableDfltMsg = true)
@SequenceGenerator(name = BudgetItem.SEQ_BUDGET_ITEM, sequenceName = BudgetItem.SEQ_BUDGET_ITEM, allocationSize = 1)
@Setter
@Getter
public class BudgetItem extends AbstractAuditable {
    public static final String TABLE_NAME = "egf_budgetitem";
    public static final String SEQ_BUDGET_ITEM = "seq_egf_budgetitem";

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetItem.class);

    @Id
    @GeneratedValue(generator = SEQ_BUDGET_ITEM, strategy = GenerationType.SEQUENCE)
    private Long id;


    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "functionid")
    private CFunction function;

    // @Column(name = "functionid")
    // private Long function;

    @ManyToOne
    @JoinColumn(name = "budgetheadid")
    private BudgetHead budgetHead;

    @ManyToOne
    @JoinColumn(name = "financialyearid")
    private CFinancialYear financialYear; // budget being created for fy

    @ManyToOne
    @JoinColumn(name = "currentfinancialyearid")
    private CFinancialYear currentFinancialYear; // budget creating on fy

    @Column(name = "budgetcode")
    private String budgetCode;

    @NotNull
    @Column(name = "budgetgroup")
    private String budgetGroup;

    @NotNull
    @Column(name = "currentestimate", precision = 13, scale = 2)
    private BigDecimal currentEstimate;

    @NotNull
    @Column(name = "currentactual",  precision = 13, scale = 2)
    private BigDecimal currentActual;

    @NotNull
    @Column(name = "currentrevisedestimate",  precision = 13, scale = 2)
    private BigDecimal currentRevisedEstimate;

    @NotNull
    @Column(name = "nextestimate",  precision = 13, scale = 2)
    private BigDecimal nextEstimate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_register_id")
    private BudgetRegister budgetRegister;


    @ManyToOne
    @JoinColumn(name = "schemeid")
    private Scheme scheme;

    @Column(name = "statebudgetcode")
    private String stateBudgetCode;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Transient
    private Integer rowIndex;


    @Column(name = "not_applicable", nullable = false)
    private Boolean notApplicable = false;


    public BigDecimal getSafeCurrentEstimate() {
         if (null != currentEstimate) {
             return  currentEstimate;
         } else {
             return BigDecimal.ZERO;
         }
    }

    public BigDecimal getSafeCurrentActual() {
        if (null != currentActual) {
            return  currentActual;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getSafeCurrentRevisedEstimate() {
        if (null != currentRevisedEstimate) {
            return  currentRevisedEstimate;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getSafeNextEstimate() {
        if (null != nextEstimate) {
            return  nextEstimate;
        } else {
            return BigDecimal.ZERO;
        }
    }

    public Boolean isValuesFilled() {
        return currentEstimate != null && currentActual != null && currentRevisedEstimate != null && nextEstimate != null;
    }


    @Transient
    public String generateBudgetCode() {

        if (budgetHead == null || function == null) {
            return null;
        }

        String functionCode = function.getCode();
        String budgetHeadCode = budgetHead.getCode();

        if (functionCode == null || budgetHeadCode == null) {
            return null;
        }

        return functionCode + "-" + budgetHeadCode;
    }




}
