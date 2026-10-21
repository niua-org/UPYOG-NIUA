package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.domain.exception.InvalidDataException;
import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.BudgetGroup;
import org.egov.egf.master.domain.model.BudgetGroupSearch;
import org.egov.egf.master.domain.model.ChartOfAccount;
import org.egov.egf.master.domain.repository.BudgetGroupRepository;
import org.egov.egf.master.domain.repository.ChartOfAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;

@ExtendWith(MockitoExtension.class)
class BudgetGroupServiceTest {

    @InjectMocks
    private BudgetGroupService budgetGroupService;

    @Mock
    private BudgetGroupRepository budgetGroupRepository;

    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "budgetGroup");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        BudgetGroup group = sampleGroup();
        when(budgetGroupRepository.save(any(BudgetGroup.class))).thenReturn(group);
        assertThat(budgetGroupService.save(group)).isSameAs(group);
    }

    @Test
    void search_returnsRepositoryResult() {
        BudgetGroupSearch search = new BudgetGroupSearch();
        search.setTenantId("default");
        Pagination<BudgetGroup> pagination = new Pagination<>();
        pagination.setPagedData(List.of(sampleGroup()));
        when(budgetGroupRepository.search(any(BudgetGroupSearch.class))).thenReturn(pagination);

        assertThat(budgetGroupService.search(search, errors)).isSameAs(pagination);
    }

    @Test
    void fetchRelated_resolvesChartOfAccounts() {
        ChartOfAccount major = ChartOfAccount.builder().id("major").build();
        BudgetGroup group = sampleGroup();
        group.setMajorCode(major);
        when(chartOfAccountRepository.findById(major)).thenReturn(major);

        List<BudgetGroup> result = budgetGroupService.fetchRelated(new ArrayList<>(List.of(group)));

        assertThat(result.get(0).getMajorCode()).isSameAs(major);
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void fetchRelated_throwsWhenMajorCodeMissing() {
        ChartOfAccount major = ChartOfAccount.builder().id("missing").build();
        BudgetGroup group = sampleGroup();
        group.setMajorCode(major);
        when(chartOfAccountRepository.findById(major)).thenReturn(null);

        assertThatThrownBy(() -> budgetGroupService.fetchRelated(new ArrayList<>(List.of(group))))
                .isInstanceOf(InvalidDataException.class);
    }

    private BudgetGroup sampleGroup() {
        BudgetGroup group = new BudgetGroup();
        group.setId("1");
        group.setName("capital");
        return group;
    }
}
