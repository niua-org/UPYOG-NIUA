package org.egov.model.repository;

import org.egov.commons.CFunction;
import org.egov.model.budget.FunctionBudgetHead;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
