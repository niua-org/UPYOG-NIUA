package org.egov.model.repository;

import org.egov.commons.CFunction;
import org.egov.model.budget.FunctionBudgetHead;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * FunctionBudgetHeadRepository provides data access operations for FunctionBudgetHead entity.
 * Manages the mapping between functions (departments) and budget heads.
 *
 * Key Features:
 * - Find all budget head mappings for a specific function/department
 * - Retrieve mappings with ID greater than specified value (for pagination/incremental loading)
 * - Search distinct functions that have budget head mappings by function name or code
 * - Custom JPQL query for case-insensitive function search across mapped budget heads
 *
 * @see FunctionBudgetHead
 * @see JpaRepository
 */
@Repository
public interface FunctionBudgetHeadRepository extends JpaRepository<FunctionBudgetHead, Long> {

    List<FunctionBudgetHead> findByFunctionId(Long functionId);

    List<FunctionBudgetHead> findByIdGreaterThan(Long id, Sort sort);

    @Query("select distinct fb.function " +
            " from FunctionBudgetHead fb " +
            "where ( lower(fb.function.name) like lower(concat('%', :query, '%'))" +
            "or lower(fb.function.code) like lower(concat('%', :query, '%')) )")
    List<CFunction> findDistinctFunctionsHavingBudgetHead(@Param("query") String query);

}
