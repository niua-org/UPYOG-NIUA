package org.egov.model.repository;

import org.egov.commons.CFinancialYear;
import org.egov.commons.EgwStatus;
import org.egov.model.budget.BudgetRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.List;

public interface BudgetRegisterWorkflowRepository extends JpaRepository<BudgetRegister, Long>  {

    @Query(value = "SELECT nextval('seq_eg_budgetregister')", nativeQuery = true)
    Long getNextBudgetRegisterSequence();


    List<BudgetRegister> findByCurrentFinancialYearAndFinancialYear(CFinancialYear currentFinancialYear, CFinancialYear financialYear);


    List<BudgetRegister> findByCurrentFinancialYearAndFinancialYearAndStatus(CFinancialYear currentFinancialYear, CFinancialYear financialYear, EgwStatus status);


    BudgetRegister findTopByCurrentFinancialYearAndFinancialYearOrderByIdDesc(CFinancialYear currentFinancialYear, CFinancialYear financialYear);


    BudgetRegister findByBudgetRegisterNumber(String budgetRegisterNumber);


}
