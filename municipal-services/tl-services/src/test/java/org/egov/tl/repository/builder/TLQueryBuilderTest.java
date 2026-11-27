package org.egov.tl.repository.builder;

import org.egov.tl.config.TLConfiguration;
import org.egov.tl.web.models.TradeLicenseSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TLQueryBuilderTest {

    @Mock
    private TLConfiguration config;

    @InjectMocks
    private TLQueryBuilder queryBuilder;

    @BeforeEach
    void stubPaginationAndBusinessService() {
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getMaxSearchLimit()).thenReturn(100);
        ReflectionTestUtils.setField(queryBuilder, "businessServiceTL", "TL");
        ReflectionTestUtils.setField(queryBuilder, "businessServiceBPA", "BPAREG");
        ReflectionTestUtils.setField(queryBuilder, "renewalPeriod", 0L);
    }

    @Test
    void getTLSearchQuery_whenTenantIdOnly_addsTenantPredicate() {
        TradeLicenseSearchCriteria criteria =
                TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getTLSearchQuery(criteria, params, false);

        assertThat(sql).contains("tl.tenantid=?");
        assertThat(params).contains("pb.amritsar", "TL");
        assertThat(params).contains(0, 10);
    }

    @Test
    void getTLSearchQuery_whenIdsGiven_bindsLowercasePlaceholders() {
        TradeLicenseSearchCriteria criteria = TradeLicenseSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .ids(List.of("ID-1", "id-2"))
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getTLSearchQuery(criteria, params, false);

        assertThat(sql).contains("tl.id IN");
        assertThat(params).contains("ID-1", "id-2");
    }

    @Test
    void getTLSearchQuery_whenCountMode_returnsCountWrapper() {
        TradeLicenseSearchCriteria criteria =
                TradeLicenseSearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getTLSearchQuery(criteria, params, true);

        assertThat(sql).contains("COUNT(DISTINCT(tl_id))");
    }

    @Test
    void getTLPlainSearchQuery_whenIdsGiven_addsIdClauseAndPagination() {
        TradeLicenseSearchCriteria criteria =
                TradeLicenseSearchCriteria.builder().ids(List.of("lic-1")).build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getTLPlainSearchQuery(criteria, params);

        assertThat(sql).contains("tl.id IN");
        assertThat(params).contains("lic-1", 0, 10);
    }
}
