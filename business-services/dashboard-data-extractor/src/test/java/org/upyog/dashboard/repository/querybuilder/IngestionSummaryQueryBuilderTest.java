package org.upyog.dashboard.repository.querybuilder;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionSummaryQueryBuilderTest {
    @Test
    void classLoads() {
        assertThat(IngestionSummaryQueryBuilder.class).isNotNull();
    }
}
