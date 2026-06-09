package org.egov.model.budget;

import lombok.Getter;
import lombok.Setter;
import org.egov.commons.CFinancialYear;
import org.egov.commons.EgwStatus;
import org.egov.infra.admin.master.entity.User;
import org.egov.infra.workflow.entity.StateAware;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BudgetRegister entity class
 *
 * This entity represents the budget register which acts as a container/document
 * for grouping multiple budget items together for approval workflow. It serves
 * as the main entry point for budget creation and approval process in the system.
 *
 * Key Features:
 * - Generates unique budget register number for tracking
 * - Groups multiple budget items for batch processing
 * - Supports workflow-based approval process through StateAware
 * - Links current and target financial years
 * - Maintains creation date and status tracking
 * - Cascade operations to associated budget items
 *
 * Budget Register Workflow:
 * 1. Created with a unique register number
 * 2. Budget items are added to the register
 * 3. Submitted for approval through workflow
 * 4. Status changes as it moves through approval hierarchy
 * 5. Finally approved/rejected by authorized personnel
 *
 * Relationship:
 * - One BudgetRegister can contain many BudgetItems (One-to-Many)
 * - Extends StateAware for workflow management
 *
 * Table: EG_BUDGETREGISTER
 * Sequence: SEQ_EG_BUDGETREGISTER
 */
@Entity
@Table(name = "EG_BUDGETREGISTER")
@SequenceGenerator(
        name = BudgetRegister.SEQ_EG_BUDGETREGISTER,
        sequenceName = BudgetRegister.SEQ_EG_BUDGETREGISTER,
        allocationSize = 1
)
@Getter
@Setter
public class BudgetRegister extends StateAware implements java.io.Serializable {

    public static final String SEQ_EG_BUDGETREGISTER = "SEQ_EG_BUDGETREGISTER";
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = SEQ_EG_BUDGETREGISTER, strategy = GenerationType.SEQUENCE)
    private Long id;

    @SafeHtml
    @Length(max = 50)
    @Column(unique = true, updatable = false, name = "budgetregisternumber")
    private String budgetRegisterNumber;

    @SafeHtml
    @Length(max = 100)
    @Column(updatable = false, name = "budgetregistername")
    private String budgetRegisterName;

    @NotNull
    private Date createdDate = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currentfinancialyearid", nullable = false)
    private CFinancialYear currentFinancialYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financialyearid", nullable = false)
    private CFinancialYear financialYear;


    @ManyToOne
    @JoinColumn(name = "statusid")
    private EgwStatus status;

//    @Column(name = "state_type", length = 100)
//    private String stateType = "BUDGET_APPROVAL";


    /** Relationship to existing BudgetItem entries */
    @OneToMany(mappedBy = "budgetRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BudgetItem> budgetItems = new LinkedHashSet<>();

    // --- Transient UI fields ---
    @Transient
    @SafeHtml
    private String approvalComent;
    @Transient
    @SafeHtml
    private String approvalDesignation;
    @Transient
    @SafeHtml
    private String approvalDepartment;
    @Transient
    @SafeHtml
    private String workFlowAction;


    @Transient
    private User createdByUser;


    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getStateDetails() {
        return getState().getComments().isEmpty() ? budgetRegisterNumber : budgetRegisterNumber + "-" + getState().getComments();
    }


}
