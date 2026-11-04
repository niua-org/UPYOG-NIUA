package org.egov.hrms.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeCountRowMapperTest {
    @Test
    void classLoads() {
        assertThat(EmployeeCountRowMapper.class).isNotNull();
    }
}
