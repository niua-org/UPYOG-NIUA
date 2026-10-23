// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.common.utils.MultiStateInstanceUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PGRUtilsTest {
    @Mock
    private MultiStateInstanceUtil multiStateInstanceUtil;

    private PGRUtils subject;

    @BeforeEach
    void setUp() {
        subject = new PGRUtils(multiStateInstanceUtil);
    }


    @Test
    void getAuditDetails_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
