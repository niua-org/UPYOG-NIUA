package org.egov.hrms.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDocumentReferenceTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(EmployeeDocumentReferenceType.values()).isNotEmpty();
    }
}
