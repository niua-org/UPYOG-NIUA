package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        AssetSearchCriteria target = new AssetSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AssetSearchCriteria()).isNotNull();
    }
}
