package org.egov.egf.instrument.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.egov.common.contract.request.User;
import org.egov.egf.instrument.domain.model.DishonorReason;
import org.egov.egf.instrument.domain.model.DishonorReasonSearch;
import org.egov.egf.instrument.web.contract.DishonorReasonContract;
import org.egov.egf.instrument.web.contract.DishonorReasonSearchContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DishonorReasonMapperTest {

    private DishonorReasonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DishonorReasonMapper();
    }

    @Test
    void toDomain_mapsAllFields() {
        DishonorReason domain = mapper.toDomain(contract());
        assertThat(domain.getId()).isEqualTo("id");
        assertThat(domain.getReason()).isEqualTo("reason-text");
        assertThat(domain.getRemarks()).isEqualTo("remarks");
        assertThat(domain.getTenantId()).isEqualTo("default");
    }

    @Test
    void toContract_mapsAllFields() {
        DishonorReasonContract contract = mapper.toContract(domain());
        assertThat(contract.getId()).isEqualTo("id");
        assertThat(contract.getReason()).isEqualTo("reason-text");
        assertThat(contract.getRemarks()).isEqualTo("remarks");
    }

    @Test
    void toSearchDomain_mapsPaginationAndFilters() {
        DishonorReasonSearch search = mapper.toSearchDomain(searchContract());
        assertThat(search.getPageSize()).isEqualTo(10);
        assertThat(search.getOffset()).isEqualTo(0);
        assertThat(search.getReason()).isEqualTo("reason-text");
    }

    @Test
    void toSearchContract_mapsPaginationAndFilters() {
        DishonorReasonSearchContract contract = mapper.toSearchContract(searchDomain());
        assertThat(contract.getPageSize()).isEqualTo(10);
        assertThat(contract.getOffset()).isEqualTo(0);
        assertThat(contract.getSortBy()).isEqualTo("reason");
    }

    private DishonorReasonContract contract() {
        DishonorReasonContract contract = new DishonorReasonContract();
        contract.setId("id");
        contract.setReason("reason-text");
        contract.setRemarks("remarks");
        contract.setTenantId("default");
        contract.setCreatedBy(User.builder().id(1L).build());
        contract.setLastModifiedBy(User.builder().id(2L).build());
        contract.setCreatedDate(new Date(1L));
        contract.setLastModifiedDate(new Date(2L));
        return contract;
    }

    private DishonorReason domain() {
        DishonorReason domain = DishonorReason.builder()
                .id("id")
                .reason("reason-text")
                .remarks("remarks")
                .build();
        domain.setTenantId("default");
        return domain;
    }

    private DishonorReasonSearchContract searchContract() {
        DishonorReasonSearchContract contract = new DishonorReasonSearchContract();
        contract.setReason("reason-text");
        contract.setPageSize(10);
        contract.setOffset(0);
        contract.setSortBy("reason");
        contract.setTenantId("default");
        return contract;
    }

    private DishonorReasonSearch searchDomain() {
        DishonorReasonSearch search = new DishonorReasonSearch();
        search.setReason("reason-text");
        search.setPageSize(10);
        search.setOffset(0);
        search.setSortBy("reason");
        search.setTenantId("default");
        return search;
    }
}
