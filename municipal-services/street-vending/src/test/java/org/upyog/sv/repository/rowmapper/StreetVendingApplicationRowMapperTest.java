// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.repository.rowmapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.sv.util.StreetVendingUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StreetVendingApplicationRowMapperTest {
    @Mock
    private StreetVendingUtil streetVendingUtil;

    private StreetVendingApplicationRowMapper subject;

    @BeforeEach
    void setUp() {
        subject = new StreetVendingApplicationRowMapper(streetVendingUtil);
    }


    @Test
    void extractData_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
