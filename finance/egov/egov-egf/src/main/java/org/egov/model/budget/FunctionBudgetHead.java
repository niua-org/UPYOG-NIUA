package org.egov.model.budget;


import lombok.Getter;
import lombok.Setter;
import org.egov.commons.CFunction;

import javax.persistence.*;


/**
 * FunctionBudgetHead entity class
 *
 * This entity represents the mapping/association between Functions (Departments/Services)
 * and Budget Heads. It acts as a bridge table to establish a many-to-many relationship,
 * defining which budget heads are applicable or allowed for which functions.
 *
 * Key Features:
 * - Links functions with budget heads
 * - Prevents duplicate mappings through unique constraint
 * - Controls which budget heads can be used by specific departments
 * - Supports budget authorization and access control
 *
 * Business Purpose:
 * This mapping ensures that:
 * - Only authorized budget heads can be used by specific departments
 * - Budget items can only be created for valid function-budgethead combinations
 * - Proper segregation of budget allocation across departments
 * - Controlled and structured budget planning
 *
 * Example Usage:
 * - Engineering Department can use "Road Maintenance" budget head
 * - Health Department can use "Medical Supplies" budget head
 * - Education Department cannot use "Road Maintenance" budget head (unless mapped)
 *
 * Table: function_budget_head
 * Unique Constraint: Combination of function_id and budget_head_id must be unique
 */
@Entity
@Table(name = "function_budget_head",
        uniqueConstraints = @UniqueConstraint(columnNames = {"budget_head_id", "function_id"}))
@Getter
@Setter
public class FunctionBudgetHead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", nullable = false)
    private CFunction function;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_head_id", nullable = false)
    private BudgetHead budgetHead;



}
