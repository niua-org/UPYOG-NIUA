package org.egov.model.repository;

import org.egov.commons.CFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.egov.model.budget.BudgetHead;

@Repository
public interface BudgetHeadRepository extends JpaRepository<BudgetHead, Long> {
    
    // List<BudgetHead> findByAccountTypeIs(String accountType);

    // List<BudgetHead> findByIsActiveTrue();


    List<BudgetHead> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);


    @Query(value = "SELECT bh.* FROM egf_budgethead bh " +
            "JOIN function_budget_head fbh ON bh.id = fbh.budget_head_id " +
            "WHERE fbh.function_id = :functionId " +
            "AND (LOWER(bh.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(bh.name) LIKE LOWER(CONCAT('%', :query, '%')))",
            nativeQuery = true)
    List<BudgetHead> searchBudgetHeadsByFunctionNative(
            @Param("functionId") Long functionId,
            @Param("query") String query);

    @Query(value = "SELECT bh.* FROM egf_budgethead bh " +
            "JOIN function_budget_head fbh ON bh.id = fbh.budget_head_id " +
            "WHERE fbh.function_id = :functionId ",
            nativeQuery = true)
    List<BudgetHead> getBudgetHeadByFunction(@Param("functionId") Long functionId);




}
