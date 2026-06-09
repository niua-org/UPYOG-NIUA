package org.egov.model.budget;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.egov.infra.persistence.entity.AbstractAuditable;
import org.egov.infra.persistence.validator.annotation.Required;
import org.egov.model.bills.EgBillregister;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;



/**
 * BudgetCoa entity class
 *
 * This entity represents the mapping between budget heads and their corresponding
 * chart of accounts. It links budget heads to specific account codes and account
 * head names for financial tracking and reporting purposes.
 *
 * Key Features:
 * - Associates budget heads with chart of accounts
 * - Maintains account code and account head information
 * - Supports audit trail through AbstractAuditable
 *
 * Table: EGF_BUDGETCOA
 * Sequence: SEQ_EGF_BUDGETCOA
 *
 */

@Entity
@Table(name = "EGF_BUDGETCOA")
@SequenceGenerator(name = BudgetCoa.SEQ_BUDGETCOA, sequenceName = BudgetCoa.SEQ_BUDGETCOA, allocationSize = 1)

public class BudgetCoa extends AbstractAuditable {

    /** Sequence name for generating primary key */
    public static final String SEQ_BUDGETCOA = "SEQ_EGF_BUDGETCOA";
    private static final long serialVersionUID = 202519091745000L;

    /**
     * Primary key - Auto-generated using sequence
     */
    @Id
    @GeneratedValue(generator = SEQ_BUDGETCOA, strategy = GenerationType.SEQUENCE)
    private Long id;


    /**
     * Reference to the budget head
     * Many BudgetCoa records can be associated with one BudgetHead
     * This is a mandatory field
     */
    @ManyToOne
    @JoinColumn(name = "budgethead_id")
    @NotNull
    private BudgetHead budgetHead;

    /**
     * Account code from the Chart of Accounts
     * This represents the GL code or account code used in financial system
     * Maximum length: 20 characters
     * This is a mandatory field
     *
     * Example: "1234567890"
     */
    @Column(name = "account_code", nullable = false, length = 20)
    @NotNull
    @Length(max = 20)
    @SafeHtml
    private String accountCode;


    /**
     * Account head name or description
     * This contains the descriptive name of the account from Chart of Accounts
     * Maximum length: 255 characters
     * This is a mandatory field
     *
     * Example: "Revenue Account - Property Tax"
     */
    @Column(name = "account_head", nullable = false, length = 255)
    @NotNull
    @Length(max = 255)
    @SafeHtml
    private String accountHead;


    // --- Getters and Setters ---
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    public BudgetHead getBudgetHead() {
        return budgetHead;
    }

    public void setBudgetHead(BudgetHead budgetHead) {
        this.budgetHead = budgetHead;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountHead() {
        return accountHead;
    }

    public void setAccountHead(String accountHead) {
        this.accountHead = accountHead;
    }
    
}
