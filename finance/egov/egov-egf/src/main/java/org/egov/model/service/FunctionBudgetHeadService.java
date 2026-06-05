package org.egov.model.service;

import org.egov.commons.CFunction;
import org.egov.model.budget.BudgetHead;
import org.egov.model.budget.FunctionBudgetHead;
import org.egov.model.repository.FunctionBudgetHeadRepository;
import org.egov.utils.BudgetAccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FunctionBudgetHeadService {

    @Autowired
    private FunctionBudgetHeadRepository functionBudgetHeadRepository;


    public List<FunctionBudgetHead> functionBudgetHeads(Long functionId) {
        return functionBudgetHeadRepository.findByFunctionId(functionId);
    }

    public List<FunctionBudgetHead> findAll() {
        return functionBudgetHeadRepository.findAll(new Sort(
                Sort.Direction.ASC, "function.code"));
    }

    public List<CFunction> getFunctionsByNameOrCode(String query) {
        return functionBudgetHeadRepository.findDistinctFunctionsHavingBudgetHead(query);
    }
    

}
