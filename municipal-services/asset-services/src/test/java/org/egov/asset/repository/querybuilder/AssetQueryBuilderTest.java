package org.egov.asset.repository.querybuilder;

import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.web.models.AssetSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetQueryBuilderTest {

    @Mock
    private AssetConfiguration assetConfiguration;

    private AssetQueryBuilder builder;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        builder = new AssetQueryBuilder(assetConfiguration);
    }

    @Test
    void getAssetSearchQuery_withTenantId_addsPredicate() {
        AssetSearchCriteria criteria = AssetSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();
        String query = builder.getAssetSearchQuery(criteria, params);
        assertThat(query).contains("tenantid");
        assertThat(params).isNotEmpty();
    }
}
