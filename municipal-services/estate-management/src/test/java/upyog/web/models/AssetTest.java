package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetTest {

    @Test
    void setAssetId_whenRoundTrip_retainsValue() {
        Asset target = new Asset();
        target.setAssetId("test");
        assertThat(target.getAssetId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Asset()).isNotNull();
    }
}
