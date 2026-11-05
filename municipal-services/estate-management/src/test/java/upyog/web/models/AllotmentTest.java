package upyog.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllotmentTest {

    @Test
    void setAllotmentId_whenRoundTrip_retainsValue() {
        Allotment target = new Allotment();
        target.setAllotmentId("test");
        assertThat(target.getAllotmentId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Allotment()).isNotNull();
    }
}
