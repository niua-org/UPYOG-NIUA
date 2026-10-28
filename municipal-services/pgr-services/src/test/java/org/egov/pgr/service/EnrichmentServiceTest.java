package org.egov.pgr.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.repository.IdGenRepository;
import org.egov.pgr.util.PGRUtils;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrichmentServiceTest {

    @Mock
    private PGRUtils utils;
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private PGRConfiguration config;
    @Mock
    private UserService userService;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @BeforeEach
    void stubPaginationDefaults() {
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getMaxLimit()).thenReturn(100);
    }

    @Test
    void enrichSearchRequest_whenCitizenEmptyCriteria_setsMobileFromUsername() {
        User citizen = User.builder().type("CITIZEN").uuid("u1").userName("9876543210").tenantId("pg.citya").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(citizen).build();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().build();

        enrichmentService.enrichSearchRequest(requestInfo, criteria);

        assertThat(criteria.getMobileNumber()).isEqualTo("9876543210");
        assertThat(criteria.getAccountId()).isEqualTo("u1");
        verify(userService).enrichUserIds("pg.citya", criteria);
    }

    @Test
    void enrichSearchRequest_appliesDefaultLimitAndOffset() {
        User employee = User.builder().type("EMPLOYEE").uuid("e1").tenantId("pg.citya").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder().tenantId("pg.citya").build();

        enrichmentService.enrichSearchRequest(requestInfo, criteria);

        assertThat(criteria.getLimit()).isEqualTo(10);
        assertThat(criteria.getOffset()).isEqualTo(0);
    }

    @Test
    void enrichSearchRequest_whenLimitAboveMax_capsToMaxLimit() {
        User employee = User.builder().type("EMPLOYEE").uuid("e1").tenantId("pg.citya").build();
        RequestInfo requestInfo = RequestInfo.builder().userInfo(employee).build();
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .limit(500)
                .offset(0)
                .build();

        enrichmentService.enrichSearchRequest(requestInfo, criteria);

        assertThat(criteria.getLimit()).isEqualTo(100);
    }
}
