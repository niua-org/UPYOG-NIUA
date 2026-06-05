package org.egov.model.budget;


import lombok.Getter;
import lombok.Setter;
import org.egov.commons.CFunction;

import javax.persistence.*;

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
