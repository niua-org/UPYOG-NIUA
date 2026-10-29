package upyog.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EstateUtilTest {

    @Test
    void parseStringToLocalDate_parsesConfiguredFormat() {
        assertThat(EstateUtil.parseStringToLocalDate("01-01-2024")).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        assertThat(EstateUtil.getAuditDetails("user", true).getCreatedBy()).isEqualTo("user");
    }
}
