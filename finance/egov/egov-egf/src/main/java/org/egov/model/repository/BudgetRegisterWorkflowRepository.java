package org.egov.model.repository;

import org.egov.commons.CFinancialYear;
import org.egov.commons.EgwStatus;
import org.egov.model.budget.BudgetRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.List;



/**
 * BudgetRegisterWorkflowRepository provides data access operations for BudgetRegister entity.
 * Manages budget register workflow operations and sequence generation.
 *
 * Key Features:
 * - Generate unique budget register sequence numbers from database sequence
 * - Find budget registers by current and target financial years
 * - Filter budget registers by workflow status (Created, Submitted, Approved, Rejected)
 * - Retrieve latest budget register for specific financial year combination
 * - Search budget registers by unique register number
 *
 * @see BudgetRegister
 * @see JpaRepository
 */
public interface BudgetRegisterWorkflowRepository extends JpaRepository<BudgetRegister, Long>  {

    @Query(value = "SELECT nextval('seq_eg_budgetregister')", nativeQuery = true)
    Long getNextBudgetRegisterSequence();


    List<BudgetRegister> findByCurrentFinancialYearAndFinancialYear(CFinancialYear currentFinancialYear, CFinancialYear financialYear);


    List<BudgetRegister> findByCurrentFinancialYearAndFinancialYearAndStatus(CFinancialYear currentFinancialYear, CFinancialYear financialYear, EgwStatus status);


    BudgetRegister findTopByCurrentFinancialYearAndFinancialYearOrderByIdDesc(CFinancialYear currentFinancialYear, CFinancialYear financialYear);


    BudgetRegister findByBudgetRegisterNumber(String budgetRegisterNumber);


}
