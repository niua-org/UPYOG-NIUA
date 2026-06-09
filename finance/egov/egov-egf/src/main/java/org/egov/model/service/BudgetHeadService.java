package org.egov.model.service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.struts2.util.SortIteratorFilter;
import org.egov.commons.CFinancialYear;
import org.egov.commons.CFunction;
import org.egov.model.budget.BudgetHead;
import org.egov.model.repository.BudgetHeadRepository;
import org.egov.utils.BudgetAccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * BudgetHeadService provides business logic for BudgetHead entity operations.
 * Handles budget head creation, retrieval, and classification management.
 *
 * Key Features:
 * - Create budget heads with automatic account type code assignment (RR, RE, CR, CE)
 * - Retrieve budget heads grouped by account type (Revenue/Capital, Receipt/Expenditure)
 * - Search budget heads by name or code with case-insensitive matching
 * - Filter budget heads by function/department association
 * - Get active budget heads sorted by display order
 * - Maps BudgetAccountType enum to short codes for database storage
 *
 * @see BudgetHead
 * @see BudgetHeadRepository
 */

@Service
@Transactional(readOnly = true)
public class BudgetHeadService {

    private final BudgetHeadRepository budgetHeadRepository;

    @Autowired
    @Qualifier("parentMessageSource")
    private MessageSource messageSource;

    @PersistenceContext
    private EntityManager entityManager;


    @Autowired
    public BudgetHeadService(final BudgetHeadRepository budgetHeadRepository) {
        this.budgetHeadRepository = budgetHeadRepository;
    }

    // @Transactional
    // public BudgetHead create(final BudgetHead budgetHead) {
    // return budgetHeadRepository.save(budgetHead);
    // }
    @Transactional
    public BudgetHead create(final BudgetHead budgetHead) {
        if (budgetHead.getAccountType() != null) {
            switch (budgetHead.getAccountType()) {
                case REVENUE_RECEIPTS:
                    budgetHead.setAccountTypeCode("RR");
                    break;
                case REVENUE_EXPENDITURE:
                    budgetHead.setAccountTypeCode("RE");
                    break;
                case CAPITAL_RECEIPTS:
                    budgetHead.setAccountTypeCode("CR");
                    break;
                case CAPITAL_EXPENDITURE:
                    budgetHead.setAccountTypeCode("CE");
                    break;
                default:
                    budgetHead.setAccountTypeCode(null); // or throw exception
            }
        }
        return budgetHeadRepository.save(budgetHead);
    }


    public void getBudgetHeadList(Model model) {
        List<BudgetHead> budgetHeads =  budgetHeadRepository.findAll();

        Map<BudgetAccountType, List<BudgetHead>> grouped = budgetHeads.stream()
                .collect(Collectors.groupingBy(BudgetHead::getAccountType));


        List<BudgetHead> revenueReceipts = grouped.get(BudgetAccountType.REVENUE_RECEIPTS);
        List<BudgetHead> revenueExpenditure = grouped.get(BudgetAccountType.REVENUE_EXPENDITURE);
        List<BudgetHead> capitalReceipts = grouped.get(BudgetAccountType.CAPITAL_RECEIPTS);
        List<BudgetHead> capitalExpenditure = grouped.get(BudgetAccountType.CAPITAL_EXPENDITURE);

        model.addAttribute("rr", revenueReceipts);
        model.addAttribute("re", revenueExpenditure);
        model.addAttribute("cr", capitalReceipts);
        model.addAttribute("ce", capitalExpenditure);

    }

    public List<BudgetHead> getActiveBudgetHeads() {
        List<BudgetHead> budgetHeads = budgetHeadRepository.findAll(new Sort(Sort.Direction.ASC, "order"));
        return budgetHeads;
    }


    public List<BudgetHead> findBudgetHeadByNameOrCode(final String query) {
        return budgetHeadRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);
    }



    public List<BudgetHead> searchBudgetHeadsByFunctionNative(final Long functionId, final String query) {
        return budgetHeadRepository.searchBudgetHeadsByFunctionNative(functionId, query);
    }

    public List<BudgetHead> getBudgetHeadsByFunction(CFunction function) {
        return budgetHeadRepository.getBudgetHeadByFunction(function.getId());
    }

    public BudgetHead findById(final  Long budgetHeadId) {
        return budgetHeadRepository.findOne(budgetHeadId);
    }


    public void getBudgetByFunctionAndFy(Long id, CFinancialYear currentFy) {

    }
}
