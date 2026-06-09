package org.egov.services.budget;


import org.egov.model.budget.BudgetItem;
import org.egov.model.repository.BudgetItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/*
 * Service for bulk budget data input operations.
 * Handles function-wise budget item creation by bulk saving budget items to the repository.
 */
@Service
@Transactional(readOnly = true)
public class BudgetInputService {

    private final BudgetItemRepository budgetItemRepository;

    private BudgetItem budgetItem;

    @Autowired
    public BudgetInputService(final BudgetItemRepository budgetItemRepository) {
        this.budgetItemRepository = budgetItemRepository;
    }


    @Transactional
    public void createFunctionWiseBudget(final List<BudgetItem> budgetItems) {

        budgetItemRepository.save(budgetItems);


    }


}
