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
import org.egov.egf.master.domain.model.Fund;
import org.egov.egf.master.domain.model.Scheme;
import org.egov.egf.master.domain.model.SchemeSearch;
import org.egov.egf.master.domain.repository.FundRepository;
import org.egov.egf.master.domain.repository.SchemeRepository;
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
class SchemeServiceTest {

    @InjectMocks
    private SchemeService schemeService;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private FundRepository fundRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "scheme");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        Scheme scheme = sample(null);
        when(schemeRepository.save(any(Scheme.class))).thenReturn(scheme);
        assertThat(schemeService.save(scheme)).isSameAs(scheme);
    }

    @Test
    void search_returnsPagination() {
        SchemeSearch search = new SchemeSearch();
        search.setTenantId("default");
        Pagination<Scheme> pagination = new Pagination<>();
        pagination.setPagedData(List.of(sample(null)));
        when(schemeRepository.search(any(SchemeSearch.class))).thenReturn(pagination);
        assertThat(schemeService.search(search, errors)).isSameAs(pagination);
    }

    @Test
    void fetchRelated_resolvesFund() {
        Fund fund = Fund.builder().id("fund").build();
        Scheme scheme = sample(fund);
        when(fundRepository.findById(fund)).thenReturn(fund);

        List<Scheme> result = schemeService.fetchRelated(new ArrayList<>(List.of(scheme)));

        assertThat(result.get(0).getFund()).isSameAs(fund);
    }

    @Test
    void fetchRelated_throwsWhenFundInvalid() {
        Fund fund = Fund.builder().id("missing").build();
        Scheme scheme = sample(fund);
        when(fundRepository.findById(fund)).thenReturn(null);

        assertThatThrownBy(() -> schemeService.fetchRelated(new ArrayList<>(List.of(scheme))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void add_assignsSequenceWhenValid() {
        Scheme scheme = sample(null);
        when(schemeRepository.uniqueCheck(eq("code"), any(Scheme.class))).thenReturn(true);
        when(schemeRepository.uniqueCheck(eq("name"), any(Scheme.class))).thenReturn(true);
        when(schemeRepository.getNextSequence()).thenReturn("sch-1");

        List<Scheme> result = schemeService.add(new ArrayList<>(List.of(scheme)), errors);

        assertThat(result.get(0).getId()).isEqualTo("sch-1");
    }

    private Scheme sample(Fund fund) {
        Scheme scheme = new Scheme();
        scheme.setId("1");
        scheme.setCode("code");
        scheme.setName("scheme");
        scheme.setFund(fund);
        return scheme;
    }
}
