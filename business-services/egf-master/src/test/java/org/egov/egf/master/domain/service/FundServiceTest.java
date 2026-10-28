package org.egov.egf.master.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.domain.model.Pagination;
import org.egov.common.util.ApplicationThreadLocals;
import org.egov.egf.master.domain.model.Fund;
import org.egov.egf.master.domain.model.FundSearch;
import org.egov.egf.master.domain.repository.FundRepository;
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
class FundServiceTest {

    @InjectMocks
    private FundService fundService;

    @Mock
    private SmartValidator validator;

    @Mock
    private FundRepository fundRepository;

    private BindingResult errors;
    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        errors = new BeanPropertyBindingResult(null, "fund");
        requestInfo = new RequestInfo();
        ApplicationThreadLocals.setTenantId("default");
    }

    @Test
    void create_whenValid_assignsIdsAndSaves() {
        List<Fund> funds = new ArrayList<>(List.of(sampleFund()));
        when(fundRepository.findById(any(Fund.class))).thenReturn(getParentFund());
        when(fundRepository.uniqueCheck(eq("name"), any(Fund.class))).thenReturn(true);
        when(fundRepository.uniqueCheck(eq("code"), any(Fund.class))).thenReturn(true);
        when(fundRepository.uniqueCheck(eq("identifier"), any(Fund.class))).thenReturn(true);
        when(fundRepository.getNextSequence()).thenReturn("seq-1");
        when(fundRepository.save(funds, requestInfo)).thenReturn(funds);

        List<Fund> result = fundService.create(funds, errors, requestInfo);

        assertThat(result).hasSize(1);
        verify(fundRepository).save(funds, requestInfo);
    }

    @Test
    void update_whenValid_updatesThroughRepository() {
        List<Fund> funds = new ArrayList<>(List.of(sampleFund()));
        when(fundRepository.findById(any(Fund.class))).thenReturn(getParentFund());
        when(fundRepository.uniqueCheck(eq("name"), any(Fund.class))).thenReturn(true);
        when(fundRepository.uniqueCheck(eq("code"), any(Fund.class))).thenReturn(true);
        when(fundRepository.uniqueCheck(eq("identifier"), any(Fund.class))).thenReturn(true);
        when(fundRepository.update(funds, requestInfo)).thenReturn(funds);

        List<Fund> result = fundService.update(funds, errors, requestInfo);

        assertThat(result).hasSize(1);
        verify(fundRepository).update(funds, requestInfo);
    }

    @Test
    void save_delegatesToRepository() {
        Fund fund = sampleFund();
        when(fundRepository.save(fund)).thenReturn(fund);
        assertThat(fundService.save(fund)).isSameAs(fund);
    }

    @Test
    void search_returnsPaginationFromRepository() {
        FundSearch fundSearch = new FundSearch();
        fundSearch.setTenantId("default");
        Pagination<Fund> expected = new Pagination<>();
        expected.setPagedData(List.of(sampleFund()));
        when(fundRepository.search(any(FundSearch.class))).thenReturn(expected);

        assertThat(fundService.search(fundSearch, errors)).isSameAs(expected);
    }

    @Test
    void updateSingle_delegatesToRepository() {
        Fund fund = sampleFund();
        when(fundRepository.update(fund)).thenReturn(fund);
        assertThat(fundService.update(fund)).isSameAs(fund);
    }

    @Test
    void fetchRelated_resolvesParentFund() {
        when(fundRepository.findById(any(Fund.class))).thenReturn(getParentFund());
        List<Fund> result = fundService.fetchRelated(new ArrayList<>(List.of(sampleFund())));
        assertThat(result.get(0).getParent().getId()).isEqualTo("2");
        assertThat(result.get(0).getTenantId()).isEqualTo("default");
    }

    private Fund sampleFund() {
        Fund fund = Fund.builder()
                .id("1")
                .name("name")
                .code("code")
                .identifier('I')
                .parent(getParentFund())
                .level(1234L)
                .active(true)
                .build();
        fund.setTenantId("default");
        return fund;
    }

    private Fund getParentFund() {
        return Fund.builder()
                .id("2")
                .name("nameP")
                .code("codeP")
                .identifier('P')
                .parent(null)
                .level(1234L)
                .active(true)
                .build();
    }
}
