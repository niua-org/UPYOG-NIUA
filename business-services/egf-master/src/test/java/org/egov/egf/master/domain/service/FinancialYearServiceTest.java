package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.FinancialYear;
import org.egov.egf.master.domain.model.FinancialYearSearch;
import org.egov.egf.master.domain.repository.FinancialYearRepository;
import org.egov.egf.master.web.requests.FinancialYearRequest;
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
class FinancialYearServiceTest {

    @InjectMocks
    private FinancialYearService financialYearService;

    @Mock
    private FinancialYearRepository financialYearRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "financialYear");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        FinancialYear year = sampleYear();
        when(financialYearRepository.save(any(FinancialYear.class))).thenReturn(year);

        assertThat(financialYearService.save(year)).isSameAs(year);
    }

    @Test
    void update_delegatesToRepository() {
        FinancialYear year = sampleYear();
        when(financialYearRepository.update(any(FinancialYear.class))).thenReturn(year);

        assertThat(financialYearService.update(year)).isSameAs(year);
    }

    @Test
    void search_returnsPaginationFromRepository() {
        FinancialYearSearch search = new FinancialYearSearch();
        search.setTenantId("default");
        Pagination<FinancialYear> expected = new Pagination<>();
        expected.setPagedData(List.of(sampleYear()));
        when(financialYearRepository.search(any(FinancialYearSearch.class))).thenReturn(expected);

        assertThat(financialYearService.search(search, errors)).isSameAs(expected);
    }

    @Test
    void add_assignsIdsWhenValid() {
        List<FinancialYear> years = new ArrayList<>(List.of(sampleYear()));
        when(financialYearRepository.uniqueCheck(eq("finYearRange"), any(FinancialYear.class))).thenReturn(true);
        when(financialYearRepository.uniqueCheck(eq("startingdate"), any(FinancialYear.class))).thenReturn(true);
        when(financialYearRepository.uniqueCheck(eq("endingdate"), any(FinancialYear.class))).thenReturn(true);
        when(financialYearRepository.getNextSequence()).thenReturn("fy-1");

        List<FinancialYear> result = financialYearService.add(years, errors);

        assertThat(result.get(0).getId()).isEqualTo("fy-1");
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void fetchRelated_setsTenantFromThreadLocal() {
        FinancialYear year = sampleYear();
        List<FinancialYear> result = financialYearService.fetchRelated(new ArrayList<>(List.of(year)));

        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void addToQue_delegatesToRepository() {
        FinancialYearRequest request = new FinancialYearRequest();
        financialYearService.addToQue(request);
        verify(financialYearRepository).add(request);
    }

    private FinancialYear sampleYear() {
        return FinancialYear.builder()
                .id("1")
                .finYearRange("2024-25")
                .startingDate(new Date())
                .endingDate(new Date())
                .active(true)
                .build();
    }
}
