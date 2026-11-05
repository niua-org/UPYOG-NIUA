package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.FinancialConfiguration;
import org.egov.egf.master.domain.model.FinancialConfigurationSearch;
import org.egov.egf.master.domain.repository.FinancialConfigurationRepository;
import org.egov.egf.master.web.requests.FinancialConfigurationRequest;
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
class FinancialConfigurationServiceTest {

    @InjectMocks
    private FinancialConfigurationService service;

    @Mock
    private FinancialConfigurationRepository repository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "financialconfiguration");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        FinancialConfiguration entity = sample();
        when(repository.save(any(FinancialConfiguration.class))).thenReturn(entity);
        assertThat(service.save(entity)).isSameAs(entity);
    }

    @Test
    void update_delegatesToRepository() {
        FinancialConfiguration entity = sample();
        when(repository.update(any(FinancialConfiguration.class))).thenReturn(entity);
        assertThat(service.update(entity)).isSameAs(entity);
    }

    @Test
    void search_returnsPaginationFromRepository() {
        FinancialConfigurationSearch search = new FinancialConfigurationSearch();
        search.setTenantId("default");
        Pagination<FinancialConfiguration> expected = new Pagination<>();
        expected.setPagedData(List.of(sample()));
        when(repository.search(any(FinancialConfigurationSearch.class))).thenReturn(expected);
        assertThat(service.search(search, errors)).isSameAs(expected);
    }

    @Test
    void add_whenValid_enrichesTenant() {
        List<FinancialConfiguration> list = new ArrayList<>(List.of(sample()));
        when(repository.uniqueCheck(eq("name"), any(FinancialConfiguration.class))).thenReturn(true);

        List<FinancialConfiguration> result = service.add(list, errors);
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void fetchRelated_setsTenantFromThreadLocal() {
        FinancialConfiguration entity = sample();
        List<FinancialConfiguration> result = service.fetchRelated(new ArrayList<>(List.of(entity)));
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void addToQue_delegatesToRepository() {
        FinancialConfigurationRequest request = new FinancialConfigurationRequest();
        service.addToQue(request);
        verify(repository).add(request);
    }

    private FinancialConfiguration sample() {
        FinancialConfiguration entity = new FinancialConfiguration();
        entity.setId("1");
        entity.setName("sample");
        entity.setModule("egf");
        return entity;
    }
}
