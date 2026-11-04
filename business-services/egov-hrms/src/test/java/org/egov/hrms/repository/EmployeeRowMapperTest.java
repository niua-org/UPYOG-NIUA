package org.egov.hrms.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeRowMapperTest {
    @Test
    void classLoads() {
        assertThat(EmployeeRowMapper.class).isNotNull();
    }
}
