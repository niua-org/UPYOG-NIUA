package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllotmentSearchCriteriaTest {

    @Test
    void setAssetNo_whenRoundTrip_retainsValue() {
        AllotmentSearchCriteria target = new AllotmentSearchCriteria();
        target.setAssetNo("test");
        assertThat(target.getAssetNo()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AllotmentSearchCriteria()).isNotNull();
    }
}
