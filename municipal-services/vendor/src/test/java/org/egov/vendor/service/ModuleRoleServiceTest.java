// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.vendor.config.ModuleRoleConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ModuleRoleServiceTest {
    @Mock
    private ModuleRoleConfig moduleRoleConfig;

    private ModuleRoleService subject;

    @BeforeEach
    void setUp() {
        subject = new ModuleRoleService(moduleRoleConfig);
    }


    @Test
    void getModuleRoleMapping_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
