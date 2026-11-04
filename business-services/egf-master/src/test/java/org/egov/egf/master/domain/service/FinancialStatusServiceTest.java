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
import org.egov.egf.master.domain.model.FinancialStatus;
import org.egov.egf.master.domain.model.FinancialStatusSearch;
import org.egov.egf.master.domain.repository.FinancialStatusRepository;
import org.egov.egf.master.web.requests.FinancialStatusRequest;
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
class FinancialStatusServiceTest {

    @InjectMocks
    private FinancialStatusService service;

    @Mock
    private FinancialStatusRepository repository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "financialstatus");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        FinancialStatus entity = sample();
        when(repository.save(any(FinancialStatus.class))).thenReturn(entity);
        assertThat(service.save(entity)).isSameAs(entity);
    }

    @Test
    void update_delegatesToRepository() {
        FinancialStatus entity = sample();
        when(repository.update(any(FinancialStatus.class))).thenReturn(entity);
        assertThat(service.update(entity)).isSameAs(entity);
    }

    @Test
    void search_returnsPaginationFromRepository() {
        FinancialStatusSearch search = new FinancialStatusSearch();
        search.setTenantId("default");
        Pagination<FinancialStatus> expected = new Pagination<>();
        expected.setPagedData(List.of(sample()));
        when(repository.search(any(FinancialStatusSearch.class))).thenReturn(expected);
        assertThat(service.search(search, errors)).isSameAs(expected);
    }

    @Test
    void add_whenValid_enrichesTenant() {
        List<FinancialStatus> list = new ArrayList<>(List.of(sample()));
        when(repository.uniqueCheck(eq("name"), any(FinancialStatus.class))).thenReturn(true);
        when(repository.uniqueCheck(eq("code"), any(FinancialStatus.class))).thenReturn(true);

        List<FinancialStatus> result = service.add(list, errors);
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void fetchRelated_setsTenantFromThreadLocal() {
        FinancialStatus entity = sample();
        List<FinancialStatus> result = service.fetchRelated(new ArrayList<>(List.of(entity)));
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void addToQue_delegatesToRepository() {
        FinancialStatusRequest request = new FinancialStatusRequest();
        service.addToQue(request);
        verify(repository).add(request);
    }

    private FinancialStatus sample() {
        FinancialStatus entity = new FinancialStatus();
        entity.setId("1");
        entity.setName("sample");
        entity.setCode("code");
        return entity;
    }
}
