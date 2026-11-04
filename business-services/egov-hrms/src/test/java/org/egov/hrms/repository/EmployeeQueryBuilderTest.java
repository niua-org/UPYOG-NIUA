package org.egov.hrms.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeQueryBuilderTest {
    @Test
    void classLoads() {
        assertThat(EmployeeQueryBuilder.class).isNotNull();
    }
}
