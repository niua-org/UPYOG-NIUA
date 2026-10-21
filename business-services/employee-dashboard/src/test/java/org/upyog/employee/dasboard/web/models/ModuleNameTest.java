package org.upyog.employee.dasboard.web.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleNameTest {
    @Test
    void values_nonEmpty() {
        assertThat(ModuleName.values()).isNotEmpty();
    }
}
