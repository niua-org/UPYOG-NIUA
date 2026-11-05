package org.upyog.employee.dasboard.query.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardConstantsTest {

    @Test
    void roleToModuleMap_containsKnownRole() {
        assertThat(DashboardConstants.ROLE_TO_MODULE_MAP.get("PGR_DASHBOARD_VIEWER"))
                .isEqualTo("PGR");
    }
}
