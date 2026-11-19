package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.domain.exception.InvalidDataException;
import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.FinancialYear;
import org.egov.egf.master.domain.model.FiscalPeriod;
import org.egov.egf.master.domain.model.FiscalPeriodSearch;
import org.egov.egf.master.domain.repository.FinancialYearRepository;
import org.egov.egf.master.domain.repository.FiscalPeriodRepository;
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
class FiscalPeriodServiceTest {

    @InjectMocks
    private FiscalPeriodService fiscalPeriodService;

    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Mock
    private FinancialYearRepository financialYearRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "fiscalPeriod");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        FiscalPeriod period = samplePeriod(null);
        when(fiscalPeriodRepository.save(any(FiscalPeriod.class))).thenReturn(period);
        assertThat(fiscalPeriodService.save(period)).isSameAs(period);
    }

    @Test
    void search_returnsPagination() {
        FiscalPeriodSearch search = new FiscalPeriodSearch();
        search.setTenantId("default");
        Pagination<FiscalPeriod> expected = new Pagination<>();
        expected.setPagedData(List.of(samplePeriod(null)));
        when(fiscalPeriodRepository.search(any(FiscalPeriodSearch.class))).thenReturn(expected);
        assertThat(fiscalPeriodService.search(search, errors)).isSameAs(expected);
    }

    @Test
    void fetchRelated_resolvesFinancialYear() {
        FinancialYear year = FinancialYear.builder().id("fy").build();
        FiscalPeriod period = samplePeriod(year);
        when(financialYearRepository.findById(year)).thenReturn(year);

        List<FiscalPeriod> result = fiscalPeriodService.fetchRelated(new ArrayList<>(List.of(period)));

        assertThat(result.get(0).getFinancialYear()).isSameAs(year);
    }

    @Test
    void fetchRelated_throwsWhenFinancialYearInvalid() {
        FinancialYear year = FinancialYear.builder().id("missing").build();
        FiscalPeriod period = samplePeriod(year);
        when(financialYearRepository.findById(year)).thenReturn(null);

        assertThatThrownBy(() -> fiscalPeriodService.fetchRelated(new ArrayList<>(List.of(period))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void add_assignsSequenceWhenValid() {
        FiscalPeriod period = samplePeriod(null);
        when(fiscalPeriodRepository.uniqueCheck(eq("name"), any(FiscalPeriod.class))).thenReturn(true);
        when(fiscalPeriodRepository.getNextSequence()).thenReturn("fp-1");

        List<FiscalPeriod> result = fiscalPeriodService.add(new ArrayList<>(List.of(period)), errors);

        assertThat(result.get(0).getId()).isEqualTo("fp-1");
    }

    private FiscalPeriod samplePeriod(FinancialYear year) {
        FiscalPeriod period = new FiscalPeriod();
        period.setId("1");
        period.setName("Q1");
        period.setFinancialYear(year);
        return period;
    }
}
