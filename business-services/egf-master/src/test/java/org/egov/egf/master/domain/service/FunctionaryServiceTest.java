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
import org.egov.egf.master.domain.model.Functionary;
import org.egov.egf.master.domain.model.FunctionarySearch;
import org.egov.egf.master.domain.repository.FunctionaryRepository;
import org.egov.egf.master.web.requests.FunctionaryRequest;
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
class FunctionaryServiceTest {

    @InjectMocks
    private FunctionaryService service;

    @Mock
    private FunctionaryRepository repository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "functionary");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        Functionary entity = sample();
        when(repository.save(any(Functionary.class))).thenReturn(entity);
        assertThat(service.save(entity)).isSameAs(entity);
    }

    @Test
    void update_delegatesToRepository() {
        Functionary entity = sample();
        when(repository.update(any(Functionary.class))).thenReturn(entity);
        assertThat(service.update(entity)).isSameAs(entity);
    }

    @Test
    void search_returnsPaginationFromRepository() {
        FunctionarySearch search = new FunctionarySearch();
        search.setTenantId("default");
        Pagination<Functionary> expected = new Pagination<>();
        expected.setPagedData(List.of(sample()));
        when(repository.search(any(FunctionarySearch.class))).thenReturn(expected);
        assertThat(service.search(search, errors)).isSameAs(expected);
    }

    @Test
    void add_whenValid_enrichesTenant() {
        List<Functionary> list = new ArrayList<>(List.of(sample()));
        when(repository.uniqueCheck(eq("name"), any(Functionary.class))).thenReturn(true);
        when(repository.uniqueCheck(eq("code"), any(Functionary.class))).thenReturn(true);
        when(repository.getNextSequence()).thenReturn("seq-1");

        List<Functionary> result = service.add(list, errors);
        assertThat(result.get(0).getId()).isEqualTo("seq-1");
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void fetchRelated_setsTenantFromThreadLocal() {
        Functionary entity = sample();
        List<Functionary> result = service.fetchRelated(new ArrayList<>(List.of(entity)));
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    @Test
    void addToQue_delegatesToRepository() {
        FunctionaryRequest request = new FunctionaryRequest();
        service.addToQue(request);
        verify(repository).add(request);
    }

    private Functionary sample() {
        Functionary entity = new Functionary();
        entity.setId("1");
        entity.setName("sample");
        entity.setCode("code");
        return entity;
    }
}
