package org.egov.model.budget.header;


import lombok.*;
import org.egov.commons.CFinancialYear;
import org.egov.infra.workflow.entity.StateAware;
import org.egov.infra.workflow.matrix.entity.WorkFlowMatrix;

import javax.persistence.*;
import java.time.LocalDateTime;

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
