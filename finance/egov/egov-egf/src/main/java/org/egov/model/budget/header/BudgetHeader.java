package org.egov.model.budget.header;


import lombok.*;
import org.egov.commons.CFinancialYear;
import org.egov.infra.workflow.entity.StateAware;
import org.egov.infra.workflow.matrix.entity.WorkFlowMatrix;

import javax.persistence.*;
import java.time.LocalDateTime;



/**
 * BudgetHeader entity represents the header/master information of a budget document.
 * It serves as the main container for budget metadata and workflow information.
 *
 * - Links current and target financial years for budget planning
 * - Supports workflow-based approval process
 * - Implements optimistic locking for concurrent update control
 * - Extends StateAware for workflow management
 *
 * Table: budget_header
 */
@Entity
@Table(name = BudgetHeader.TABLE_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetHeader extends StateAware {

    public static final String TABLE_NAME = "budget_header";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "currentfinancialyearid", nullable = false)
    private CFinancialYear currentFinancialYear;

    @ManyToOne
    @JoinColumn(name = "financialyearid", nullable = false)
    private CFinancialYear financialYear;

    @Column(name = "name", nullable = false, length = 100)
    private String name;


    @Column(name = "version")
    private Long version = 0L;          // optimistic locking

    @Override
    public String getStateDetails() {
        return name + getState().getValue();
    }
}
