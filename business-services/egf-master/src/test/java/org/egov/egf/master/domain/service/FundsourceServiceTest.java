package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.egov.common.domain.exception.InvalidDataException;
import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.Fundsource;
import org.egov.egf.master.domain.model.FundsourceSearch;
import org.egov.egf.master.domain.repository.FundsourceRepository;
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
class FundsourceServiceTest {

    @InjectMocks
    private FundsourceService fundsourceService;

    @Mock
    private FundsourceRepository fundsourceRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "fundsource");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        Fundsource fundsource = sample(null);
        when(fundsourceRepository.save(any(Fundsource.class))).thenReturn(fundsource);
        assertThat(fundsourceService.save(fundsource)).isSameAs(fundsource);
    }

    @Test
    void search_returnsPagination() {
        FundsourceSearch search = new FundsourceSearch();
        search.setTenantId("default");
        Pagination<Fundsource> pagination = new Pagination<>();
        pagination.setPagedData(List.of(sample(null)));
        when(fundsourceRepository.search(any(FundsourceSearch.class))).thenReturn(pagination);
        assertThat(fundsourceService.search(search, errors)).isSameAs(pagination);
    }

    @Test
    void fetchRelated_resolvesParent() {
        Fundsource parent = sample(null);
        parent.setId("parent");
        Fundsource child = sample(parent);
        when(fundsourceRepository.findById(parent)).thenReturn(parent);

        List<Fundsource> result = fundsourceService.fetchRelated(new ArrayList<>(List.of(child)));

        assertThat(result.get(0).getParent()).isSameAs(parent);
    }

    @Test
    void fetchRelated_throwsWhenParentInvalid() {
        Fundsource parent = sample(null);
        Fundsource child = sample(parent);
        when(fundsourceRepository.findById(parent)).thenReturn(null);

        assertThatThrownBy(() -> fundsourceService.fetchRelated(new ArrayList<>(List.of(child))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void add_assignsSequenceWhenValid() {
        Fundsource fundsource = sample(null);
        when(fundsourceRepository.uniqueCheck(eq("code"), any(Fundsource.class))).thenReturn(true);
        when(fundsourceRepository.getNextSequence()).thenReturn("fs-1");

        List<Fundsource> result = fundsourceService.add(new ArrayList<>(List.of(fundsource)), errors);

        assertThat(result.get(0).getId()).isEqualTo("fs-1");
    }

    private Fundsource sample(Fundsource parent) {
        return Fundsource.builder()
                .id("1")
                .code("code")
                .name("name")
                .active(true)
                .llevel(BigDecimal.ONE)
                .parent(parent)
                .build();
    }
}
