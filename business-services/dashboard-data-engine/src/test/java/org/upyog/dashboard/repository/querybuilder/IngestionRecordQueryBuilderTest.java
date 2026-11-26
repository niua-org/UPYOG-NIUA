package org.upyog.dashboard.repository.querybuilder;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionRecordQueryBuilderTest {
    @Test
    void classLoads() {
        assertThat(IngestionRecordQueryBuilder.class).isNotNull();
    }
}
