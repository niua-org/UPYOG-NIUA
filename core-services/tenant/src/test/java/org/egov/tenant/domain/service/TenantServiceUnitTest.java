package org.egov.tenant.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tenant.domain.exception.DuplicateTenantCodeException;
import org.egov.tenant.domain.model.City;
import org.egov.tenant.domain.model.Tenant;
import org.egov.tenant.persistence.repository.MdmsRepository;
import org.egov.tenant.persistence.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceUnitTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private MdmsRepository mdmsRepository;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new TenantService(tenantRepository);
        ReflectionTestUtils.setField(tenantService, "mdmsRepository", mdmsRepository);
    }

    @Test
    void createTenant_validTenant_returnsSavedTenant() {
        Tenant tenant = validTenant();
        when(tenantRepository.isTenantPresent(tenant.getCode())).thenReturn(0L);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        Tenant result = tenantService.createTenant(tenant);

        assertThat(result).isEqualTo(tenant);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void getTenants_mdmsReturnsNull_returnsEmptyList() {
        List<String> codes = Collections.emptyList();
        RequestInfo requestInfo = RequestInfo.builder().apiId("tenant").action("search").build();

        List<Tenant> result = tenantService.getTenants(codes, requestInfo);

        assertThat(result).isEmpty();
        verify(mdmsRepository).getByCriteria(null, null, null, null, codes, requestInfo);
    }

    @Test
    void createTenant_duplicateCode_throwsDuplicateTenantCodeException() {
        Tenant tenant = validTenant();
        when(tenantRepository.isTenantPresent(tenant.getCode())).thenReturn(1L);

        assertThatThrownBy(() -> tenantService.createTenant(tenant))
                .isInstanceOf(DuplicateTenantCodeException.class);

        verify(tenantRepository, never()).save(tenant);
    }

    private static Tenant validTenant() {
        City city = City.builder().name("Amritsar").ulbGrade("Municipal Corporation").build();
        return Tenant.builder()
                .code("pb.amritsar")
                .name("Amritsar")
                .logoId("logo-1")
                .imageId("image-1")
                .type("CITY")
                .city(city)
                .build();
    }
}
