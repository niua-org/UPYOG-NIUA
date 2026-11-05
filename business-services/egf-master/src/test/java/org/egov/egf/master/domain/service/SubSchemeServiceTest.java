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
import org.egov.egf.master.domain.model.Scheme;
import org.egov.egf.master.domain.model.SubScheme;
import org.egov.egf.master.domain.model.SubSchemeSearch;
import org.egov.egf.master.domain.repository.SchemeRepository;
import org.egov.egf.master.domain.repository.SubSchemeRepository;
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
class SubSchemeServiceTest {

    @InjectMocks
    private SubSchemeService subSchemeService;

    @Mock
    private SubSchemeRepository subSchemeRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SmartValidator validator;

    private BindingResult errors;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "subScheme");
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void save_delegatesToRepository() {
        SubScheme subScheme = sample(null);
        when(subSchemeRepository.save(any(SubScheme.class))).thenReturn(subScheme);
        assertThat(subSchemeService.save(subScheme)).isSameAs(subScheme);
    }

    @Test
    void search_returnsPagination() {
        SubSchemeSearch search = new SubSchemeSearch();
        search.setTenantId("default");
        Pagination<SubScheme> pagination = new Pagination<>();
        pagination.setPagedData(List.of(sample(null)));
        when(subSchemeRepository.search(any(SubSchemeSearch.class))).thenReturn(pagination);
        assertThat(subSchemeService.search(search, errors)).isSameAs(pagination);
    }

    @Test
    void fetchRelated_resolvesScheme() {
        Scheme scheme = new Scheme();
        scheme.setId("scheme");
        SubScheme subScheme = sample(scheme);
        when(schemeRepository.findById(scheme)).thenReturn(scheme);

        List<SubScheme> result = subSchemeService.fetchRelated(new ArrayList<>(List.of(subScheme)));

        assertThat(result.get(0).getScheme()).isSameAs(scheme);
    }

    @Test
    void fetchRelated_throwsWhenSchemeInvalid() {
        Scheme scheme = new Scheme();
        scheme.setId("missing");
        SubScheme subScheme = sample(scheme);
        when(schemeRepository.findById(scheme)).thenReturn(null);

        assertThatThrownBy(() -> subSchemeService.fetchRelated(new ArrayList<>(List.of(subScheme))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void add_assignsSequenceWhenValid() {
        SubScheme subScheme = sample(null);
        when(subSchemeRepository.uniqueCheck(eq("code"), any(SubScheme.class))).thenReturn(true);
        when(subSchemeRepository.getNextSequence()).thenReturn("ss-1");

        List<SubScheme> result = subSchemeService.add(new ArrayList<>(List.of(subScheme)), errors);

        assertThat(result.get(0).getId()).isEqualTo("ss-1");
    }

    private SubScheme sample(Scheme scheme) {
        SubScheme subScheme = new SubScheme();
        subScheme.setId("1");
        subScheme.setCode("code");
        subScheme.setName("sub");
        subScheme.setScheme(scheme);
        return subScheme;
    }
}
